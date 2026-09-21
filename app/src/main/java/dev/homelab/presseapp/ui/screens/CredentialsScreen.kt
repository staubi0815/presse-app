package dev.homelab.presseapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.homelab.presseapp.data.SecureCredentialStore

/**
 * Einmalige Eingabe der VOEBB-Zugangsdaten (Ausweisnummer + PIN). Wird
 * verschluesselt gespeichert (s. SecureCredentialStore) und danach vom
 * LoginAutomator automatisch verwendet - der Nutzer muss diesen Screen im
 * Normalfall nur einmal sehen.
 */
@Composable
fun CredentialsScreen(
    credentialStore: SecureCredentialStore,
    onSaved: () -> Unit,
) {
    var ausweisnummer by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "VÖBB-Zugangsdaten",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Werden verschlüsselt auf diesem Gerät gespeichert und nur für den automatischen Login verwendet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            OutlinedTextField(
                value = ausweisnummer,
                onValueChange = { ausweisnummer = it },
                label = { Text("Ausweisnummer") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Button(
                onClick = {
                    credentialStore.save(ausweisnummer.trim(), pin.trim())
                    onSaved()
                },
                enabled = ausweisnummer.isNotBlank() && pin.isNotBlank(),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 20.dp),
            ) {
                Text("Speichern")
            }
        }
    }
}
