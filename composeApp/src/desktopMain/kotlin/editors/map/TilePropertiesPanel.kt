package editors.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.TileProperties

val TILE_TYPES = listOf(
    "GRASS", "WATER", "MOUNTAIN", "CAVE", "ROAD", "SAND",
    "SNOW", "DESERT", "FOREST", "ICE", "SWAMP", "LAVA"
)

private val FPV_SLOTS = listOf(
    "NORTH_BOUNDARY", "EAST_BOUNDARY", "SOUTH_BOUNDARY", "WEST_BOUNDARY",
    "CEILING", "FLOOR", "CLOSE_FOREGROUND", "FOREGROUND", "FAR_FOREGROUND"
)
private val DIRECTIONS = listOf("NORTH", "EAST", "SOUTH", "WEST")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TilePropertiesPanel(
    coord: String?,
    tile: TileProperties?,
    tileImageFiles: List<String>,
    textureImageFiles: List<String>,
    onUpdate: (TileProperties) -> Unit,
    onAddTile: () -> Unit,
    onRemoveTile: () -> Unit,
    autoAddBoundaries: Boolean,
    autoAddTiles: Boolean,
    autoUpdateAdjacentTiles: Boolean,
    onAutoAddBoundariesChange: (Boolean) -> Unit,
    onAutoAddTilesChange: (Boolean) -> Unit,
    onAutoUpdateAdjacentTilesChange: (Boolean) -> Unit,
    hideLegend: Boolean,
    onHideLegendChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (coord == null) {
        Box(modifier = modifier.padding(16.dp)) {
            Text("Click a tile to select it", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = hideLegend, onCheckedChange = onHideLegendChange)
                Text("Hide Legend", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        TileHeader(coord, tile, onRemoveTile)

        if (tile == null) {
            EmptyTileActions(
                onAddTile = onAddTile,
                autoAddBoundaries = autoAddBoundaries,
                autoAddTiles = autoAddTiles,
                autoUpdateAdjacentTiles = autoUpdateAdjacentTiles,
                onAutoAddBoundariesChange = onAutoAddBoundariesChange,
                onAutoAddTilesChange = onAutoAddTilesChange,
                onAutoUpdateAdjacentTilesChange = onAutoUpdateAdjacentTilesChange
            )
            return@Column
        }

        TileTypeDropdown(tile, onUpdate)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = tile.disabled, onCheckedChange = { onUpdate(tile.copy(disabled = it)) })
            Text("Disabled")
        }

        DropdownTextField(
            value = tile.minimapTile,
            onValueChange = { onUpdate(tile.copy(minimapTile = it)) },
            label = "Minimap Tile",
            options = tileImageFiles,
            modifier = Modifier.fillMaxWidth()
        )
        DropdownTextField(
            value = tile.minimapIcon,
            onValueChange = { onUpdate(tile.copy(minimapIcon = it)) },
            label = "Minimap Icon",
            options = emptyList(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ingress", style = MaterialTheme.typography.labelLarge)
                DirectionToggles(values = tile.ingress) { onUpdate(tile.copy(ingress = it)) }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Boundaries", style = MaterialTheme.typography.labelLarge)
                DirectionToggles(values = tile.drawBoundaries) { onUpdate(tile.copy(drawBoundaries = it)) }
            }
        }

        EventsDropdown()
        TroopsDropdown()

        FpvTexturesSection(tile, textureImageFiles, onUpdate)
    }
}

@Composable
private fun TileHeader(coord: String, tile: TileProperties?, onRemoveTile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val parts = coord.split(",")
        val coordLabel = if (parts.size == 2) "Coordinates: (${parts[0]}, ${parts[1]})" else "Coordinates: $coord"
        Text(coordLabel, style = MaterialTheme.typography.titleSmall)

        if (tile != null) {
            var showDeleteDialog by remember { mutableStateOf(false) }
            Button(
                onClick = { showDeleteDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) { Text("Delete Tile", style = MaterialTheme.typography.labelMedium) }
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Tile") },
                    text = { Text("Delete tile at ($coord)? This cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = { showDeleteDialog = false; onRemoveTile() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyTileActions(
    onAddTile: () -> Unit,
    autoAddBoundaries: Boolean,
    autoAddTiles: Boolean,
    autoUpdateAdjacentTiles: Boolean,
    onAutoAddBoundariesChange: (Boolean) -> Unit,
    onAutoAddTilesChange: (Boolean) -> Unit,
    onAutoUpdateAdjacentTilesChange: (Boolean) -> Unit
) {
    Text("No tile at this position", style = MaterialTheme.typography.bodySmall)
    Button(onClick = onAddTile) { Text("Add Tile") }
    AutoCheckbox(
        checked = autoAddBoundaries,
        onCheckedChange = onAutoAddBoundariesChange,
        label = "Automatically Add Boundaries",
        tooltip = "Sets boundary walls on sides adjacent to empty space and enables ingress from existing tiles. Also scans adjacent tiles outward ring by ring to find the most common boundary FPV texture, applying it to each walled side."
    )
    AutoCheckbox(
        checked = autoAddTiles,
        onCheckedChange = onAutoAddTilesChange,
        label = "Automatically Add Tiles",
        tooltip = "Copies type, ceiling, floor, and minimap tile from adjacent tiles. Scans outward ring by ring, ranks candidates by prevalence with alphabetical tiebreaking, and expands further out if nothing is found nearby."
    )
    AutoCheckbox(
        checked = autoUpdateAdjacentTiles,
        onCheckedChange = onAutoUpdateAdjacentTilesChange,
        label = "Automatically Update Adjacent Tiles",
        tooltip = "Removes boundary walls and enables ingress to this tile from any neighboring tile that faces it."
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TileTypeDropdown(tile: TileProperties, onUpdate: (TileProperties) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = tile.type,
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TILE_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = { onUpdate(tile.copy(type = type)); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun FpvTexturesSection(
    tile: TileProperties,
    textureImageFiles: List<String>,
    onUpdate: (TileProperties) -> Unit
) {
    Text("FPV Textures", style = MaterialTheme.typography.labelLarge)
    FPV_SLOTS.forEach { slot ->
        val current = tile.fpvTextures[slot]?.firstOrNull() ?: ""
        DropdownTextField(
            value = current,
            onValueChange = { newVal ->
                val list = if (newVal.isBlank()) emptyList() else listOf(newVal)
                onUpdate(tile.copy(fpvTextures = tile.fpvTextures + (slot to list)))
            },
            label = slot.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            options = textureImageFiles,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DimmedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val isEmpty = value.isBlank()
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val normalColor = MaterialTheme.colorScheme.onSurface
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (isEmpty) dimColor else normalColor),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedLabelColor = if (isEmpty) dimColor else MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedBorderColor = if (isEmpty) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    val isEmpty = value.isBlank()
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val normalColor = MaterialTheme.colorScheme.onSurface
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value.substringAfterLast('/'),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (isEmpty) dimColor else normalColor),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedLabelColor = if (isEmpty) dimColor else MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedBorderColor = if (isEmpty) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (value.isNotBlank() && value !in options) {
                DropdownMenuItem(
                    text = { Text(value.substringAfterLast('/')) },
                    onClick = { onValueChange(value); expanded = false }
                )
                HorizontalDivider()
            }
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.substringAfterLast('/')) },
                    onClick = { onValueChange(opt); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    tooltip: String
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(isPersistent = true)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsDropdown() {
    Text("Events", style = MaterialTheme.typography.labelLarge)
    ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Events") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(false) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TroopsDropdown() {
    Text("Troops", style = MaterialTheme.typography.labelLarge)
    ExposedDropdownMenuBox(expanded = false, onExpandedChange = {}) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Troops") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(false) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
    }
}

@Composable
private fun DirectionToggles(values: Map<String, String>, onChange: (Map<String, String>) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(-12.dp)) {
        Text("N", style = MaterialTheme.typography.labelSmall)
        DirectionCheckbox("NORTH", values, onChange)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(-8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            Text("W", style = MaterialTheme.typography.labelSmall)
            DirectionCheckbox("WEST", values, onChange)
            DirectionCheckbox("EAST", values, onChange)
            Text("E", style = MaterialTheme.typography.labelSmall)
        }
        DirectionCheckbox("SOUTH", values, onChange)
        Text("S", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DirectionCheckbox(dir: String, values: Map<String, String>, onChange: (Map<String, String>) -> Unit) {
    val checked = values[dir]?.lowercase() == "true"
    Checkbox(checked = checked, onCheckedChange = { onChange(values + (dir to it.toString())) })
}
