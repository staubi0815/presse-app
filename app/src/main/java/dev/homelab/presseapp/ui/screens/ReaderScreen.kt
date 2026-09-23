package dev.homelab.presseapp.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import dev.homelab.presseapp.BuildConfig
import dev.homelab.presseapp.data.SecureCredentialStore
import dev.homelab.presseapp.data.Source
import dev.homelab.presseapp.webview.AllowlistWebViewClient

/**
 * Zeigt die echte Original-Seite der gewaehlten Quelle live in einer
 * WebView - kein Download, kein eigener Nachbau. Der LoginAutomator
 * (via AllowlistWebViewClient) haelt den Login automatisch am Laufen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderScreen(
    source: Source,
    credentialStore: SecureCredentialStore,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val surfaceColorArgb = MaterialTheme.colorScheme.surface.toArgb()

    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(source.label)
                        if (currentUrl.isNotBlank()) {
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val wv = webViewRef
                        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onHome) {
                        Icon(Icons.Filled.Home, contentDescription = "Zur Übersicht")
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Neu laden")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    Text("Seite konnte nicht geladen werden", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    Button(onClick = {
                        errorMessage = null
                        webViewRef?.reload()
                    }) {
                        Text("Erneut versuchen")
                    }
                }
            } else {
                // Compose's PullToRefreshBox erkennt Zieh-Gesten innerhalb einer
                // klassischen android.webkit.WebView nicht zuverlaessig (WebView
                // nimmt nicht am Compose-NestedScroll-System teil) - deshalb hier
                // die View-basierte SwipeRefreshLayout direkt um die WebView, das
                // ist der etablierte Weg fuer Pull-to-Refresh mit WebView.
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        if (BuildConfig.DEBUG) {
                            WebView.setWebContentsDebuggingEnabled(true)
                        }
                        val webView = WebView(context).apply {
                            setBackgroundColor(surfaceColorArgb)

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.setSupportMultipleWindows(true)
                            // Ohne diese beiden Einstellungen ignoriert WebView das
                            // <meta name="viewport"> der Seite und layoutet stattdessen
                            // mit einer synthetischen Desktop-Breite. Bei Muenzinger fuehrt
                            // das dazu, dass der position:fixed-Hauptcontainer der Vue-App
                            // auf Hoehe 0 kollabiert (leere weisse Seite trotz vorhandenem
                            // Inhalt im DOM).
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            // Pinch-to-Zoom fuer kleine Genios/Muenzinger-Texte; die
                            // eingebauten +/- Buttons bleiben aus, Pinch reicht.
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false

                            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                                WebSettingsCompat.setSafeBrowsingEnabled(settings, true)
                            }

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = AllowlistWebViewClient(
                                credentialStore = credentialStore,
                                onLoadingStateChanged = { loading ->
                                    isLoading = loading
                                    if (loading) errorMessage = null
                                },
                                onUrlChanged = { url -> currentUrl = url },
                                onMainFrameError = { message -> errorMessage = message },
                            )
                            // VOEBB-SSO/OIDC-Login (Muenzinger) oeffnet den Login-Schritt per
                            // window.open() in einem neuen Fenster - ohne diesen Handler
                            // verschluckt die WebView das stillschweigend (Nutzer haengt an
                            // einem endlosen Lade-Spinner fest). Popup-Inhalt wird stattdessen
                            // einfach in derselben WebView geladen.
                            webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message,
                                ): Boolean {
                                    val transport = resultMsg.obj as WebView.WebViewTransport
                                    transport.webView = view
                                    resultMsg.sendToTarget()
                                    return true
                                }

                                // Standardmaessig gibt WebView JS-console.*-Ausgaben NICHT an
                                // logcat weiter - ohne diesen Override sind clientseitige
                                // Fehler der Zielseite unsichtbar.
                                override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                                    android.util.Log.w(
                                        "WebViewConsole",
                                        "${message.messageLevel()} ${message.sourceId()}:${message.lineNumber()}: ${message.message()}",
                                    )
                                    return true
                                }
                            }

                            // Genios' "ePaper PDF"-Button (c't/IX) liefert einen echten
                            // PDF-Download - ohne DownloadListener verschluckt die WebView
                            // das stillschweigend (kein sichtbarer Effekt beim Antippen).
                            // Cookie manuell mitgeben, da DownloadManager ausserhalb des
                            // WebView-Cookiejars laeuft und sonst nur die Login-Seite laden
                            // wuerde statt der eigentlichen (angemeldeten) PDF-Datei.
                            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                val request = DownloadManager.Request(Uri.parse(url)).apply {
                                    addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                                    addRequestHeader("User-Agent", userAgent)
                                    setMimeType(mimeType)
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                                    setTitle(fileName)
                                }
                                val downloadManager = context.getSystemService(DownloadManager::class.java)
                                downloadManager.enqueue(request)
                                Toast.makeText(context, "Download gestartet: $fileName", Toast.LENGTH_SHORT).show()
                            }

                            webViewRef = this
                            loadUrl(source.entryUrl)
                        }

                        SwipeRefreshLayout(context).apply {
                            addView(webView)
                            setOnRefreshListener { webView.reload() }
                        }
                    },
                    update = { swipeRefresh -> swipeRefresh.isRefreshing = isLoading },
                )
            }
        }
    }
}
