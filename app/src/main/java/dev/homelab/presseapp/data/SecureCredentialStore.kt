package dev.homelab.presseapp.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.credentialsDataStore by preferencesDataStore(name = "voebb_credentials")

/**
 * Haelt die VOEBB-Zugangsdaten (Ausweisnummer + PIN) verschluesselt auf dem
 * Geraet. `EncryptedSharedPreferences` (androidx.security:security-crypto)
 * ist seit 1.1.0 offiziell deprecated (Performance-/Keyset-Korruptions-
 * Probleme) - aktueller Standard ist stattdessen Jetpack DataStore fuer die
 * Ablage + Google Tink fuer die Verschluesselung + Android Keystore fuer
 * den Schluesselschutz (Tink verwaltet dabei selbst einen Keyset, dessen
 * eigener Schluessel wiederum durch einen Keystore-Eintrag geschuetzt ist -
 * "envelope encryption").
 *
 * Die Werte selbst liegen nur Base64-kodiert + Tink-verschluesselt in
 * DataStore, nie im Klartext. Zugangsdaten werden nie geloggt.
 *
 * API bewusst synchron gehalten (kleine lokale Datei, keine Netzwerk-E/A) -
 * `runBlocking` fuer zwei kurze String-Werte ist hier vernachlässigbar
 * und vermeidet, dass Coroutines durch CredentialsScreen/Navigation/
 * LoginAutomator durchgereicht werden muessen.
 */
class SecureCredentialStore(private val context: Context) {

    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "voebb_credentials_keyset", "voebb_credentials_keyset_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://dev.homelab.presseapp.credentials_master_key")
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    data class Credentials(val ausweisnummer: String, val pin: String)

    private fun encrypt(plaintext: String): String {
        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), null)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        return String(aead.decrypt(ciphertext, null), Charsets.UTF_8)
    }

    fun hasCredentials(): Boolean = load() != null

    fun load(): Credentials? = runBlocking {
        val prefs = context.credentialsDataStore.data.first()
        val ausweisEnc = prefs[KEY_AUSWEIS] ?: return@runBlocking null
        val pinEnc = prefs[KEY_PIN] ?: return@runBlocking null
        Credentials(decrypt(ausweisEnc), decrypt(pinEnc))
    }

    fun save(ausweisnummer: String, pin: String) {
        runBlocking {
            context.credentialsDataStore.edit { prefs ->
                prefs[KEY_AUSWEIS] = encrypt(ausweisnummer)
                prefs[KEY_PIN] = encrypt(pin)
            }
        }
    }

    fun clear() {
        runBlocking {
            context.credentialsDataStore.edit { it.clear() }
        }
    }

    private companion object {
        val KEY_AUSWEIS = stringPreferencesKey("ausweisnummer")
        val KEY_PIN = stringPreferencesKey("pin")
    }
}
