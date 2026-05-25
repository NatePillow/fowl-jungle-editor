package editors.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private val COLOR_SELECTED = Color(0xFFFFEB3B)
private val COLOR_TROOP    = Color(0xFFE53935)
private val COLOR_EVENT    = Color(0xFFFFD600)

@Composable
fun MapEditor(
    state: MapEditorState,
    tileImageFiles: List<String>,
    textureImageFiles: List<String>,
    canvasLabel: String? = null,
) {
    val map = state.currentMap
    if (map == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No map files found in the opened project")
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp).clip(RectangleShape)) {
                if (canvasLabel != null) {
                    Text(
                        text = canvasLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp).zIndex(1f)
                    )
                }
                MapCanvas(
                    mapData = map,
                    selectedCoord = state.selectedCoord,
                    onSelectTile = { state.selectedCoord = if (it == state.selectedCoord) null else it },
                    onExpand = { state.expand(it) },
                    modifier = Modifier.fillMaxSize()
                )
                if (!state.hideLegend) {
                    MapLegend(modifier = Modifier.align(Alignment.BottomEnd).zIndex(1f))
                }
            }

            VerticalDivider()

            TilePropertiesPanel(
                coord = state.selectedCoord,
                tile = state.selectedTile,
                tileImageFiles = tileImageFiles,
                textureImageFiles = textureImageFiles,
                onUpdate = { updated -> state.selectedCoord?.let { state.updateTile(it, updated) } },
                onAddTile = { state.selectedCoord?.let { state.addTile(it) } },
                onRemoveTile = { state.selectedCoord?.let { state.removeTile(it) } },
                autoAddBoundaries = state.autoAddBoundaries,
                autoAddTiles = state.autoAddTiles,
                autoUpdateAdjacentTiles = state.autoUpdateAdjacentTiles,
                onAutoAddBoundariesChange = { state.autoAddBoundaries = it },
                onAutoAddTilesChange = { state.autoAddTiles = it },
                onAutoUpdateAdjacentTilesChange = { state.autoUpdateAdjacentTiles = it },
                hideLegend = state.hideLegend,
                onHideLegendChange = { state.hideLegend = it },
                modifier = Modifier.width(340.dp).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    val wallColor = MaterialTheme.colorScheme.onSurface
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            LegendDotRow("Troop spawn", COLOR_TROOP)
            LegendDotRow("Event", COLOR_EVENT)
            LegendOutlineRow("Selected tile", COLOR_SELECTED)
            LegendLineRow("Wall, no entry", wallColor, pathEffect = null)
            LegendLineRow("Wall, entry allowed", wallColor,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 2f)))
            LegendLineRow("No wall, no entry", wallColor,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 2f)))
        }
    }
}

@Composable
private fun LegendDotRow(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(14.dp)) {
            drawCircle(color, radius = size.minDimension * 0.35f)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LegendOutlineRow(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(14.dp)) {
            drawRect(color, style = Stroke(2f))
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LegendLineRow(label: String, color: Color, pathEffect: PathEffect?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(14.dp, 14.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height * 0.18f,
                pathEffect = pathEffect
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
