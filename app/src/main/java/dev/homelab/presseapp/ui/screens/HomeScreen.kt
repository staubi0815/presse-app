package dev.homelab.presseapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.homelab.presseapp.data.Source

/**
 * Uebersicht der vier Quellen als grosszuegiges Karten-Grid (Material 3).
 * Bei nur 4 Eintraegen bewusst kein Nav-Drawer/Bottom-Nav - das Grid allein
 * ist bereits uebersichtlich genug.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSourceSelected: (Source) -> Unit,
    onEditCredentials: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presse") },
                actions = {
                    IconButton(onClick = onEditCredentials) {
                        Icon(Icons.Filled.Settings, contentDescription = "Zugangsdaten bearbeiten")
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            items(Source.entries) { source ->
                SourceCard(source = source, onClick = { onSourceSelected(source) })
            }
        }
    }
}

@Composable
private fun SourceCard(source: Source, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(source.accentColorHex)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.15f))
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
            ) {
                Text(
                    text = source.label,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
