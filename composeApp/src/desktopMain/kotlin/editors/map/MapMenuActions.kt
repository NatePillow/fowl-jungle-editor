@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package editors.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import AppState

@Composable
fun MapMenuActions(state: AppState) {
    val hasMap = state.currentFile != null

    Button(onClick = { state.showNewMapDialog = true }) { Text("New") }

    Button(
        onClick = { state.openRenameDialog() },
        enabled = hasMap && !state.mapEditorState.dirty
    ) { Text("Rename") }

    Button(
        onClick = { state.saveMap() },
        enabled = hasMap && state.mapEditorState.dirty
    ) { Text("Save") }

    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 16.dp))

    Button(
        onClick = { },
        enabled = false
    ) { Text("Event Editor") }

    Button(
        onClick = { },
        enabled = false
    ) { Text("Troop Editor") }

    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 16.dp))

    Button(
        onClick = { state.showResetDialog = true },
        enabled = hasMap && state.mapEditorState.dirty && state.currentFile?.exists() == true
    ) { Text("Reset") }

    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 16.dp))

    Button(
        onClick = { state.showDeleteDialog = true },
        enabled = hasMap
    ) { Text("Delete Map") }
}

@Composable
fun NewMapDialog(state: AppState) {
    AlertDialog(
        onDismissRequest = { state.dismissNewMapDialog() },
        title = { Text("New Map") },
        text = { NewMapDialogContent(state) },
        confirmButton = {
            Button(enabled = state.newMapFileName.isNotBlank(), onClick = { state.createMap() }) { Text("OK") }
        },
        dismissButton = {
            OutlinedButton(onClick = { state.dismissNewMapDialog() }) { Text("Cancel") }
        }
    )
}

@Composable
private fun NewMapDialogContent(state: AppState) {
    var tileTypeExpanded by remember { mutableStateOf(false) }
    var minimapTileExpanded by remember { mutableStateOf(false) }
    var boundaryTextureExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.newMapName,
            onValueChange = { state.newMapName = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.newMapFileName,
            onValueChange = { state.newMapFileName = it },
            label = { Text("File name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.newMapWidth,
                onValueChange = { state.newMapWidth = it.filter { c -> c.isDigit() } },
                label = { Text("Starting Tile Width") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.newMapHeight,
                onValueChange = { state.newMapHeight = it.filter { c -> c.isDigit() } },
                label = { Text("Starting Tile Height") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        SimpleDropdown(
            label = "Tile Type",
            value = state.newMapTileType,
            options = TILE_TYPES,
            expanded = tileTypeExpanded,
            onExpandedChange = { tileTypeExpanded = it },
            onSelect = { state.newMapTileType = it }
        )
        SimpleDropdown(
            label = "Minimap Tile",
            value = state.newMapMinimapTile,
            options = state.project.tileImageFiles,
            expanded = minimapTileExpanded,
            onExpandedChange = { minimapTileExpanded = it },
            onSelect = { state.newMapMinimapTile = it }
        )
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = state.newMapAutoBoundaries,
                onCheckedChange = { state.newMapAutoBoundaries = it }
            )
            Text("Automatically Add Boundaries", style = MaterialTheme.typography.bodyMedium)
        }
        val boundaryOptions = state.project.textureImageFiles.let { list ->
            if (state.newMapBoundaryTexture !in list) listOf(state.newMapBoundaryTexture) + list else list
        }
        SimpleDropdown(
            label = "Boundary Texture",
            value = state.newMapBoundaryTexture,
            options = boundaryOptions,
            enabled = state.newMapAutoBoundaries,
            expanded = boundaryTextureExpanded,
            onExpandedChange = { if (state.newMapAutoBoundaries) boundaryTextureExpanded = it },
            onSelect = { state.newMapBoundaryTexture = it }
        )
    }
}

@Composable
private fun SimpleDropdown(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange, modifier = modifier) {
        OutlinedTextField(
            value = value.substringAfterLast('/'),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.substringAfterLast('/')) },
                    onClick = { onSelect(opt); onExpandedChange(false) }
                )
            }
        }
    }
}

@Composable
fun RenameMapDialog(state: AppState) {
    AlertDialog(
        onDismissRequest = { state.dismissRenameDialog() },
        title = { Text("Rename Map") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.renameMapName,
                    onValueChange = { state.renameMapName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.renameMapFileName,
                    onValueChange = { state.renameMapFileName = it },
                    label = { Text("File name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = state.renameMapFileName.isNotBlank(),
                onClick = { state.renameMap() }
            ) { Text("Rename") }
        },
        dismissButton = {
            OutlinedButton(onClick = { state.dismissRenameDialog() }) { Text("Cancel") }
        }
    )
}

@Composable
fun ResetMapDialog(state: AppState) {
    val name = state.currentFile?.nameWithoutExtension ?: "this map"
    AlertDialog(
        onDismissRequest = { state.showResetDialog = false },
        title = { Text("Reset Map") },
        text = { Text("Reset \"$name\" to the last saved version? All unsaved changes will be lost.") },
        confirmButton = {
            Button(onClick = { state.showResetDialog = false; state.resetMap() }) { Text("Reset") }
        },
        dismissButton = {
            OutlinedButton(onClick = { state.showResetDialog = false }) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteMapDialog(state: AppState) {
    val name = state.currentFile?.nameWithoutExtension ?: "this map"
    AlertDialog(
        onDismissRequest = { state.showDeleteDialog = false },
        title = { Text("Delete Map") },
        text = { Text("Delete \"$name\"? This cannot be undone.") },
        confirmButton = {
            Button(
                onClick = { state.deleteMap() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            OutlinedButton(onClick = { state.showDeleteDialog = false }) { Text("Cancel") }
        }
    )
}
