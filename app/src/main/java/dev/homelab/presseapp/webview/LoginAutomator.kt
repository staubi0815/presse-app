package dev.homelab.presseapp.webview

import android.net.Uri
import android.webkit.WebView
import dev.homelab.presseapp.data.SecureCredentialStore
import org.json.JSONObject

/**
 * Kotlin/WebView-Pendant zu den bereits bewaehrten Login-Funktionen in
 * bot.py (voebb_login, voebb_genios_login, munzinger_login). Statt
 * Playwright-Python-Klicks werden dieselben Schritte per injiziertem
 * JavaScript in der echten WebView ausgefuehrt.
 *
 * Wird bei JEDEM Seitenaufbau (WebViewClient.onPageFinished) aufgerufen und
 * startet dort einen kurzlebigen Polling-Loop (statt eines einmaligen
 * Checks): Muenzingers Login-Modal ist eine Vue-SPA, bei der Tab-Wechsel
 * (z.B. "Mit Bibliotheksausweis") und Zwischenschritte NICHT als echter
 * Seitenaufbau zaehlen - onPageFinished wuerde also nur den allerersten
 * Schritt sehen und dann nie wieder feuern. Der Loop scannt deshalb alle
 * ~400ms auf neu erschienene Marker, bis entweder eine echte Navigation
 * den Dokumentkontext (und damit den Loop) beendet, oder ein Sicherheitslimit
 * erreicht ist.
 *
 * Sicherheits-Vorkehrung: die Zugangsdaten-Injection laeuft nur, wenn sowohl
 * die Domain als auch die exakten VOEBB-Feldnamen gefunden werden - kein
 * blindes "erstes Passwortfeld auf der Seite" Verhalten.
 */
object LoginAutomator {

    private val ALLOWED_HOSTS = setOf(
        "voebb.de", "www.voebb.de",
        "bib-voebb.genios.de",
        // "munzinger.de" deckt per endsWith-Logik unten auch die
        // OIDC-Login-Weiterleitung ueber www.munzinger.de mit ab (bisher nur
        // online.munzinger.de erlaubt, wodurch genau dieser Redirect
        // (/search/oidc/auth/...) stillschweigend blockiert wurde).
        "munzinger.de",
    )

    fun isAllowedHost(url: String): Boolean {
        val host = Uri.parse(url).host ?: return false
        return ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    fun runNextStep(webView: WebView, url: String, credentialStore: SecureCredentialStore) {
        if (!isAllowedHost(url)) return
        val creds = credentialStore.load() ?: return

        val ausweisJson = JSONObject.quote(creds.ausweisnummer)
        val pinJson = JSONObject.quote(creds.pin)

        val js = """
            (function() {
                if (window.__presseAppAutopilot) { return; }
                window.__presseAppAutopilot = true;

                function clickOnce(el) {
                    if (!el || el.dataset.presseAppClicked === '1') { return false; }
                    el.dataset.presseAppClicked = '1';
                    el.click();
                    return true;
                }

                function clickByText(text, tags) {
                    for (var t = 0; t < tags.length; t++) {
                        var els = document.querySelectorAll(tags[t]);
                        for (var i = 0; i < els.length; i++) {
                            if (els[i].textContent && els[i].textContent.trim() === text) {
                                if (clickOnce(els[i])) { return true; }
                            }
                        }
                    }
                    return false;
                }

                function tryStep() {
                    // Regel A: Sitzung wurde beendet -> neue Sitzung starten
                    if (clickOnce(document.querySelector('a.endsession'))) { return; }

                    // Regel F: Muenzinger-Login-Modal auf "Institution/Firma/
                    // Kunde"-Tab statt "Mit Bibliotheksausweis" -> umschalten
                    if (document.querySelector('.login-modal') &&
                        !document.querySelector("input[name='L#AUSW']") &&
                        clickByText('Mit Bibliotheksausweis', ['a', 'button', '[role="tab"]', 'li', 'div', 'span'])) {
                        return;
                    }

                    // Regel E: Muenzinger-Login-Modal "Weiter"
                    if (clickOnce(document.querySelector('.login-modal .login-navigation button.btn-primary'))) { return; }

                    // Regel D: Genios/Muenzinger-Consent (CLOGIN)
                    if (clickOnce(document.querySelector("[name='CLOGIN']"))) { return; }

                    // Regel C: VOEBB-Login-Formular direkt befuellen + absenden
                    var ausw = document.querySelector("input[name='L#AUSW']");
                    var pass = document.querySelector("input[name='LPASSW']");
                    if (ausw && pass && !ausw.value) {
                        ausw.value = $ausweisJson;
                        pass.value = $pinJson;
                        var submit = document.querySelector("input[name='LLOGIN']");
                        if (submit) { clickOnce(submit); } else { ausw.form && ausw.form.submit(); }
                        return;
                    }

                    // Regel G: Login erfolgreich, aber die OIDC-Weiterleitung landet
                    // auf der Muenzinger-Portal-Startseite ("/") statt direkt auf der
                    // Spiegel-Publikation -> einmalig dorthin weiterleiten.
                    if (location.hostname.endsWith('munzinger.de') &&
                        location.pathname === '/' &&
                        !document.querySelector('.login-modal') &&
                        !window.__presseAppRedirectedToPub) {
                        var params = new URLSearchParams(location.search);
                        var portalid = params.get('portalid');
                        if (portalid) {
                            window.__presseAppRedirectedToPub = true;
                            location.href = 'https://online.munzinger.de/publikation/spiegel?portalid=' + portalid;
                            return;
                        }
                    }

                    // Regel B: "Mein Konto"-Link (fuehrt erst zum Login-Formular)
                    clickByText('Mein Konto', ['a']);

                    // Regel H: Muenzinger-Publikationsseite geladen, Inhalt steht im DOM,
                    // aber der position:fixed-Hauptcontainer der Vue-App bleibt leer
                    // (per DevTools-Diagnose bestaetigt: Hoehe 0 trotz vorhandenem
                    // Inhalt). Ein nachtraeglich ausgeloestes resize-Event bringt
                    // Layout-Berechnungen, die von window.innerWidth/Height abhaengen,
                    // oft wieder zum Laufen - billiger, ungefaehrlicher Versuch.
                    if (location.hostname.endsWith('munzinger.de') &&
                        location.pathname.indexOf('/publikation/') === 0 &&
                        !window.__presseAppResizeNudge) {
                        window.__presseAppResizeNudge = true;
                        window.dispatchEvent(new Event('resize'));
                        window.dispatchEvent(new Event('orientationchange'));
                    }
                }

                var attempts = 0;
                var handle = setInterval(function() {
                    attempts++;
                    tryStep();
                    if (attempts > 50) { clearInterval(handle); }
                }, 400);
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
