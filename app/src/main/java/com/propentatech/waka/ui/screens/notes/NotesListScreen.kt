package com.propentatech.waka.ui.screens.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.propentatech.waka.data.local.entity.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(notes: List<Note>, onNoteClick: (Long) -> Unit, onCreateNote: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Bloc-notes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle fiche")
            }
        },
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Aucune fiche pour l'instant. Touchez + pour en créer une.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { onNoteClick(note.id) }) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(note.title.ifBlank { "Sans titre" }, style = MaterialTheme.typography.titleMedium)
                            if (note.content.isNotBlank()) {
                                Text(
                                    note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
