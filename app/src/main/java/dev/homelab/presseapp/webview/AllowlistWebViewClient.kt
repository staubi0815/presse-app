package dev.homelab.presseapp.webview

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dev.homelab.presseapp.data.SecureCredentialStore

/**
 * Beschraenkt die WebView auf die bekannten VOEBB/Genios/Muenzinger-Domains
 * (+ das oeffentliche Spiegel-Titelbild-Asset) und stoesst nach jedem
 * Seitenaufbau den LoginAutomator an. Verhindert, dass Links innerhalb der
 * Seite zu einer beliebigen Fremd-Domain navigieren - wichtig gerade weil
 * hier automatisiert Zugangsdaten unterwegs sind.
 */
class AllowlistWebViewClient(
    private val credentialStore: SecureCredentialStore,
    private val onLoadingStateChanged: (Boolean) -> Unit,
    private val onUrlChanged: (String) -> Unit,
) : WebViewClient() {

    private val extraAllowedHosts = setOf("magazin.spiegel.de")

    private fun isAllowed(url: String): Boolean {
        if (LoginAutomator.isAllowedHost(url)) return true
        val host = Uri.parse(url).host ?: return false
        return extraAllowedHosts.any { host == it || host.endsWith(".$it") }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (!isAllowed(url)) {
            // Fremd-Domain: nicht in der WebView oeffnen, einfach ignorieren.
            Log.w("AllowlistWebView", "Blockiert (nicht auf Allowlist): $url")
            return true
        }
        return false
    }

    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
        onLoadingStateChanged(true)
        onUrlChanged(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        onLoadingStateChanged(false)
        onUrlChanged(url)
        LoginAutomator.runNextStep(view, url, credentialStore)
        forceRepaint(view)
    }

    // Bekannter Android-WebView-Bug: nach bestimmten Navigationen (v.a. per
    // JS location.href statt echtem Link-Klick, wie bei unserer Regel G)
    // bleibt der GPU-Compositor-Layer leer, obwohl das DOM/Layout korrekt
    // ist - per DevTools-Diagnose bestaetigt (Inhalt vorhanden, Bounding-Box
    // korrekt, aber nichts gezeichnet). Kurzes Umschalten des Layer-Typs
    // erzwingt einen sauberen Neuzeichnen-Durchlauf.
    private fun forceRepaint(view: WebView) {
        view.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        view.postDelayed({ view.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null) }, 150)
        // Bei SPAs (Vue etc.) steht der eigentliche Inhalt oft erst nach
        // onPageFinished per Client-Side-Rendering - ein zweiter, spaeterer
        // Durchlauf faengt auch diesen Fall ab.
        view.postDelayed({
            view.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            view.postDelayed({ view.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null) }, 150)
        }, 1000)
    }
}
