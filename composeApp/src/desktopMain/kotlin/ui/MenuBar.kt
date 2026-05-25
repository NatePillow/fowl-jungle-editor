package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import javax.swing.JFileChooser
import kotlin.system.exitProcess
import AppState

@Composable
fun AppMenuBar(
    state: AppState,
    editorActions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Open Project (select assets/json folder)"
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                state.project.open(chooser.selectedFile)
            }
        }) { Text("Open Project") }

        Button(
            onClick = { state.showReloadDialog = true },
            enabled = state.project.isOpen
        ) { Text("Reload Project") }

        state.project.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (editorActions != null) {
            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 16.dp))
            editorActions()
        }

        Spacer(Modifier.weight(1f))

        SettingsMenu(state)
    }
}

@Composable
private fun SettingsMenu(state: AppState) {
    IconButton(onClick = { state.showSettingsDialog = true }) {
        Icon(Icons.Default.Settings, contentDescription = "Settings")
    }
}

@Composable
fun SettingsDialog(state: AppState) {
    AlertDialog(
        onDismissRequest = { state.showSettingsDialog = false },
        modifier = Modifier.width(240.dp),
        title = { Text("Main Menu", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { state.isDark = !state.isDark },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.isDark) "Light Mode" else "Dark Mode") }

                Button(
                    onClick = { state.showSettingsDialog = false; state.showInitializeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Initialize") }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { exitProcess(0) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Exit") }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun InitializeDialog(state: AppState) {
    AlertDialog(
        onDismissRequest = { state.showInitializeDialog = false },
        title = { Text("Initialize") },
        text = { Text("Reset all settings and restore the sample project to its original state?") },
        confirmButton = {
            Button(
                onClick = { state.initialize() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Initialize") }
        },
        dismissButton = {
            OutlinedButton(onClick = { state.showInitializeDialog = false }) { Text("Cancel") }
        }
    )
}

@Composable
fun ReloadConfirmDialog(state: AppState) {
    AlertDialog(
        onDismissRequest = { state.showReloadDialog = false },
        title = { Text("Reload Project") },
        text = { Text("Reload all files from disk? Any unsaved changes will be lost.") },
        confirmButton = {
            Button(onClick = {
                state.showReloadDialog = false
                state.project.root?.let { state.project.open(it) }
            }) { Text("Reload") }
        },
        dismissButton = {
            OutlinedButton(onClick = { state.showReloadDialog = false }) { Text("Cancel") }
        }
    )
}
