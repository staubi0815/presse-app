package dev.homelab.presseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.homelab.presseapp.data.ReaderStateStore
import dev.homelab.presseapp.data.SecureCredentialStore
import dev.homelab.presseapp.ui.theme.PresseAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val credentialStore = SecureCredentialStore(applicationContext)
        val readerStateStore = ReaderStateStore(applicationContext)

        setContent {
            PresseAppTheme {
                AppNavHost(credentialStore = credentialStore, readerStateStore = readerStateStore)
            }
        }
    }
}
