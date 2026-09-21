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
 * Wird bei JEDEM Seitenaufbau (WebViewClient.onPageFinished) aufgerufen -
 * ist die Sitzung noch gueltig, findet keine der Regeln unten einen
 * passenden Marker und es passiert nichts. Ist sie abgelaufen, greift
 * genau eine Regel pro Seitenaufbau; der dadurch ausgeloeste Klick fuehrt
 * zu einem neuen Seitenaufbau, bei dem dann die naechste Regel greift -
 * dieselbe schrittweise Kette wie in bot.py, nur clientseitig.
 *
 * Sicherheits-Vorkehrung: die Zugangsdaten-Injection (Regel C) laeuft nur,
 * wenn sowohl die Domain als auch die exakten VOEBB-Feldnamen gefunden
 * werden - kein blindes "erstes Passwortfeld auf der Seite" Verhalten.
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

        // Jede Regel prueft selbst, ob ihr Marker vorhanden ist, und meldet
        // per Rueckgabewert, ob sie etwas getan hat - abgebrochen wird nach
        // der ersten erfolgreichen Regel (mirror des sequentiellen Ablaufs
        // in bot.py).
        val js = """
            (function() {
                function clickIfPresent(selector) {
                    var el = document.querySelector(selector);
                    if (el) { el.click(); return true; }
                    return false;
                }

                // Regel A: Sitzung wurde beendet -> neue Sitzung starten
                if (clickIfPresent('a.endsession')) { return 'endsession'; }

                // Regel E: Muenzinger-Login-Modal "Weiter"
                if (clickIfPresent('.login-modal .login-navigation button.btn-primary')) {
                    return 'muenzinger-modal';
                }

                // Regel D: Genios/Muenzinger-Consent (CLOGIN)
                if (clickIfPresent("[name='CLOGIN']")) { return 'consent'; }

                // Regel C: VOEBB-Login-Formular direkt befuellen + absenden
                var ausw = document.querySelector("input[name='L#AUSW']");
                var pass = document.querySelector("input[name='LPASSW']");
                if (ausw && pass) {
                    ausw.value = $ausweisJson;
                    pass.value = $pinJson;
                    var submit = document.querySelector("input[name='LLOGIN']");
                    if (submit) { submit.click(); } else { ausw.form && ausw.form.submit(); }
                    return 'voebb-login';
                }

                // Regel B: "Mein Konto"-Link (fuehrt erst zum Login-Formular)
                var links = document.querySelectorAll('a');
                for (var i = 0; i < links.length; i++) {
                    if (links[i].textContent && links[i].textContent.trim() === 'Mein Konto') {
                        links[i].click();
                        return 'mein-konto';
                    }
                }

                return null;
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
