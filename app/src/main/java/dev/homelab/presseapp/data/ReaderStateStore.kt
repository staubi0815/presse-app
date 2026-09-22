package dev.homelab.presseapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.readerStateDataStore by preferencesDataStore(name = "reader_state")

/**
 * Merkt sich pro Quelle die zuletzt besuchte URL (z.B. eine bestimmte
 * Ausgabe/Rubrik), damit man nach dem Zurueckkehren nicht wieder beim
 * Einstiegspunkt landet. Unkritische, unverschluesselte Ablage (nur
 * Fach-URLs der Verlagsseiten, keine Zugangsdaten) - bewusst synchron
 * wie SecureCredentialStore, aus denselben Gruenden.
 */
class ReaderStateStore(private val context: Context) {

    fun loadLastUrl(sourceName: String): String? = runBlocking {
        context.readerStateDataStore.data.first()[keyFor(sourceName)]
    }

    fun saveLastUrl(sourceName: String, url: String) {
        runBlocking {
            context.readerStateDataStore.edit { prefs -> prefs[keyFor(sourceName)] = url }
        }
    }

    private fun keyFor(sourceName: String) = stringPreferencesKey("last_url_$sourceName")
}
