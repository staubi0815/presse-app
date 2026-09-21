package dev.homelab.presseapp.data

/**
 * Die vier unterstuetzten Quellen mit ihrem jeweiligen Einstiegspunkt.
 * URLs stammen aus dem bereits bewaehrten paywall-bot (bot.py) - dort
 * wurden Login-Fluss und Navigationspfade ueber Playwright reverse-
 * engineered, hier wird derselbe Einstiegspunkt einfach live im Browser
 * (WebView) geoeffnet statt gescraped.
 */
enum class Source(
    val label: String,
    val entryUrl: String,
    val accentColorHex: Long,
) {
    SPIEGEL(
        label = "Spiegel",
        entryUrl = "https://online.munzinger.de/publikation/spiegel?portalid=50158",
        accentColorHex = 0xFFEA4100,
    ),
    CT(
        label = "c't",
        entryUrl = "https://bib-voebb.genios.de/toc_list/CT",
        accentColorHex = 0xFFD32F2F,
    ),
    IX(
        label = "IX",
        entryUrl = "https://bib-voebb.genios.de/toc_list/IX",
        accentColorHex = 0xFF1565C0,
    ),
    FAZ(
        // Kein bestaetigter toc_list-Code fuer FAZ dokumentiert (lief bisher
        // ueber generische Genios-Dokumentsuche) - Startpunkt ist deshalb
        // die Genios-Startseite; bei Bedarf spaeter auf einen praeziseren
        // Deep-Link anpassen, sobald der echte FAZ-Presse-Code bekannt ist.
        label = "FAZ",
        entryUrl = "https://bib-voebb.genios.de/",
        accentColorHex = 0xFF37474F,
    ),
}
