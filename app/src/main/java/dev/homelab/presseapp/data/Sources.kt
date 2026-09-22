package dev.homelab.presseapp.data

import dev.homelab.presseapp.R

/**
 * Die unterstuetzten Quellen mit ihrem jeweiligen Einstiegspunkt.
 * URLs stammen aus dem bereits bewaehrten paywall-bot (bot.py) - dort
 * wurden Login-Fluss und Navigationspfade ueber Playwright reverse-
 * engineered, hier wird derselbe Einstiegspunkt einfach live im Browser
 * (WebView) geoeffnet statt gescraped.
 */
enum class Source(
    val label: String,
    val entryUrl: String,
    val accentColorHex: Long,
    val logoRes: Int,
) {
    SPIEGEL(
        label = "Spiegel",
        entryUrl = "https://online.munzinger.de/publikation/spiegel?portalid=50158",
        accentColorHex = 0xFFEA4100,
        logoRes = R.drawable.logo_spiegel,
    ),
    CT(
        label = "c't",
        entryUrl = "https://bib-voebb.genios.de/toc_list/CT",
        accentColorHex = 0xFFD32F2F,
        logoRes = R.drawable.logo_ct,
    ),
    IX(
        label = "IX",
        entryUrl = "https://bib-voebb.genios.de/toc_list/IX",
        accentColorHex = 0xFF1565C0,
        logoRes = R.drawable.logo_ix,
    ),
    HANDELSBLATT(
        label = "Handelsblatt",
        entryUrl = "https://bib-voebb.genios.de/toc_list/HBLATE",
        accentColorHex = 0xFF002F6C,
        logoRes = R.drawable.logo_handelsblatt,
    ),
    SZ(
        label = "Süddeutsche Zeitung",
        entryUrl = "https://bib-voebb.genios.de/toc_list/SZ",
        accentColorHex = 0xFF1B4F72,
        logoRes = R.drawable.logo_sz,
    ),
    TSP(
        label = "Tagesspiegel",
        entryUrl = "https://bib-voebb.genios.de/toc_list/TSP",
        accentColorHex = 0xFFB0202E,
        logoRes = R.drawable.logo_tsp,
    ),
    ZEIT(
        label = "Die Zeit",
        entryUrl = "https://bib-voebb.genios.de/toc_list/ZEIT",
        accentColorHex = 0xFF7A9A3F,
        logoRes = R.drawable.logo_zeit,
    ),
}
