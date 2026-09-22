package dev.homelab.presseapp.webview

import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
    private val onMainFrameError: (String) -> Unit,
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
    }

    // Nur Hauptdokument-Fehler melden (nicht jede blockierte/fehlgeschlagene
    // Nebenressource wie Tracking-Skripte o.ae.) - sonst wuerde staendig ein
    // Fehlerbildschirm aufpoppen, obwohl die eigentliche Seite laedt.
    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) {
            onMainFrameError(error.description?.toString() ?: "Unbekannter Fehler")
        }
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) {
            onMainFrameError("HTTP ${errorResponse.statusCode}")
        }
    }
}
