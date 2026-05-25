package editors.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import model.MapCoord
import model.MapData
import model.TileProperties

private val TILE_SIZE = 56.dp
private const val CANVAS_PADDING = 1
private const val ZOOM_MIN = 0.5f
private const val ZOOM_MAX = 2.0f
private const val ZOOM_STEP = 0.1f

private val TILE_COLORS = mapOf(
    "GRASS" to Color(0xFF4CAF50),
    "WATER" to Color(0xFF2196F3),
    "MOUNTAIN" to Color(0xFF795548),
    "CAVE" to Color(0xFF424242),
    "ROAD" to Color(0xFFBCAAA4),
    "SAND" to Color(0xFFFFF176),
    "SNOW" to Color(0xFFE3F2FD),
    "DESERT" to Color(0xFFFFCC80),
    "FOREST" to Color(0xFF2E7D32),
    "ICE" to Color(0xFFB3E5FC),
    "SWAMP" to Color(0xFF558B2F),
    "LAVA" to Color(0xFFBF360C)
)
private val COLOR_SELECTED = Color(0xFFFFEB3B)
private val COLOR_TROOP = Color(0xFFE53935)
private val COLOR_EVENT = Color(0xFFFFD600)

private val DIRECTIONS = listOf("NORTH", "SOUTH", "EAST", "WEST")

data class CanvasGrid(
    val minX: Int, val minY: Int,
    val maxX: Int, val maxY: Int,
    val cols: Int, val rows: Int
)

private fun computeGrid(mapData: MapData, allCoords: List<Pair<MapCoord, String>>): CanvasGrid {
    val tileMinX = allCoords.minOf { it.first.x }
    val tileMaxX = allCoords.maxOf { it.first.x }
    val tileMinY = allCoords.minOf { it.first.y }
    val tileMaxY = allCoords.maxOf { it.first.y }
    val b = mapData.bounds
    val effMinX = if (b != null) minOf(tileMinX, b.minX) else tileMinX
    val effMaxX = if (b != null) maxOf(tileMaxX, b.maxX) else tileMaxX
    val effMinY = if (b != null) minOf(tileMinY, b.minY) else tileMinY
    val effMaxY = if (b != null) maxOf(tileMaxY, b.maxY) else tileMaxY
    val minX = effMinX - CANVAS_PADDING
    val minY = effMinY - CANVAS_PADDING
    val maxX = effMaxX + CANVAS_PADDING
    val maxY = effMaxY + CANVAS_PADDING
    return CanvasGrid(minX, minY, maxX, maxY, cols = maxX - minX + 1, rows = maxY - minY + 1)
}

private fun DrawScope.drawGrid(grid: CanvasGrid, ts: Float, colorEmpty: Color, colorGrid: Color) {
    for (x in 0 until grid.cols) {
        for (y in 0 until grid.rows) {
            val left = x * ts
            val top = y * ts
            drawRect(colorEmpty, Offset(left, top), Size(ts, ts))
            drawRect(colorGrid, Offset(left, top), Size(ts, ts), style = Stroke(1f))
        }
    }
}

private fun DrawScope.drawTiles(
    allCoords: List<Pair<MapCoord, String>>,
    tileProperties: Map<String, TileProperties>,
    selectedCoord: String?,
    grid: CanvasGrid,
    ts: Float,
    wallColor: Color,
    colorDisabled: Color,
    colorUnknown: Color
) {
    val wallWidth = ts * 0.1f
    val half = wallWidth / 2f
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(ts * 0.3f, ts * 0.1f))
    val dotEffect  = PathEffect.dashPathEffect(floatArrayOf(ts * 0.06f, ts * 0.1f))

    for ((coord, key) in allCoords) {
        val tile = tileProperties[key] ?: continue
        val left = (coord.x - grid.minX) * ts
        val top = (grid.maxY - coord.y) * ts
        val color = if (tile.disabled) colorDisabled else (TILE_COLORS[tile.type] ?: colorUnknown)

        drawRect(color, Offset(left, top), Size(ts, ts))

        if (!tile.disabled) {
            DIRECTIONS.forEach { dir ->
                val hasBoundary = tile.drawBoundaries[dir]?.lowercase() == "true"
                val hasIngress  = tile.ingress[dir]?.lowercase() == "true"
                val pathEffect = when {
                    hasBoundary && !hasIngress -> null
                    hasBoundary &&  hasIngress -> dashEffect
                    !hasBoundary && !hasIngress -> dotEffect
                    else -> return@forEach
                }
                when (dir) {
                    "NORTH" -> drawLine(wallColor, Offset(left,              top + half),      Offset(left + ts, top + half),      wallWidth, pathEffect = pathEffect)
                    "SOUTH" -> drawLine(wallColor, Offset(left,              top + ts - half), Offset(left + ts, top + ts - half), wallWidth, pathEffect = pathEffect)
                    "EAST"  -> drawLine(wallColor, Offset(left + ts - half,  top),             Offset(left + ts - half, top + ts), wallWidth, pathEffect = pathEffect)
                    "WEST"  -> drawLine(wallColor, Offset(left + half,       top),             Offset(left + half,      top + ts), wallWidth, pathEffect = pathEffect)
                }
            }
        }

        if (key != selectedCoord) {
            drawRect(Color.Black.copy(alpha = 0.2f), Offset(left, top), Size(ts, ts), style = Stroke(1f))
        }
    }
}

private fun DrawScope.drawSelection(selectedCoord: String?, grid: CanvasGrid, ts: Float) {
    selectedCoord?.let { sel ->
        runCatching { MapCoord.parse(sel) }.getOrNull()?.let { mc ->
            val left = (mc.x - grid.minX) * ts
            val top = (grid.maxY - mc.y) * ts
            drawRect(COLOR_SELECTED, Offset(left, top), Size(ts, ts), style = Stroke(3f))
        }
    }
}

private fun DrawScope.drawMarkers(
    troopCoords: List<MapCoord>,
    eventCoords: List<MapCoord>,
    grid: CanvasGrid,
    ts: Float
) {
    fun center(coord: MapCoord): Offset {
        val left = (coord.x - grid.minX) * ts
        val top = (grid.maxY - coord.y) * ts
        return Offset(left + ts * 0.5f, top + ts * 0.5f)
    }
    for (coord in troopCoords) drawCircle(COLOR_TROOP, radius = ts * 0.16f, center = center(coord))
    for (coord in eventCoords) drawCircle(COLOR_EVENT, radius = ts * 0.16f, center = center(coord))
}

@Composable
private fun ChevronOverlay(
    cols: Int,
    rows: Int,
    panOffset: Offset,
    zoomScale: Float,
    onExpand: (ExpandDirection) -> Unit
) {
    val density = LocalDensity.current
    val tilesDp = TILE_SIZE * zoomScale
    // Box is 80dp larger than the canvas (40dp per side). Centering places it
    // 40dp outside each canvas edge, so chevrons sit within layout bounds and
    // hit-testing works reliably — no per-chevron offset needed.
    Box(
        modifier = Modifier
            .size(tilesDp * cols + 80.dp, tilesDp * rows + 80.dp)
            .offset(
                x = with(density) { panOffset.x.toDp() },
                y = with(density) { panOffset.y.toDp() }
            )
    ) {
        IconButton(
            onClick = { onExpand(ExpandDirection.North) },
            modifier = Modifier.align(Alignment.TopCenter).width(80.dp).height(40.dp)
        ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }

        IconButton(
            onClick = { onExpand(ExpandDirection.South) },
            modifier = Modifier.align(Alignment.BottomCenter).width(80.dp).height(40.dp)
        ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }

        IconButton(
            onClick = { onExpand(ExpandDirection.West) },
            modifier = Modifier.align(Alignment.CenterStart).width(40.dp).height(80.dp)
        ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) }

        IconButton(
            onClick = { onExpand(ExpandDirection.East) },
            modifier = Modifier.align(Alignment.CenterEnd).width(40.dp).height(80.dp)
        ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
    }
}

@Composable
fun MapCanvas(
    mapData: MapData,
    selectedCoord: String?,
    onSelectTile: (String) -> Unit,
    onExpand: (ExpandDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCoords = mapData.tileProperties.keys.mapNotNull { key ->
        runCatching { MapCoord.parse(key) to key }.getOrNull()
    }
    if (allCoords.isEmpty()) return

    val grid = computeGrid(mapData, allCoords)
    val troopCoords = mapData.troops
        .filter { (_, spawns) -> spawns.any { it.id.isNotBlank() } }
        .keys.mapNotNull { runCatching { MapCoord.parse(it) }.getOrNull() }
    val eventCoords = mapData.events
        .filter { (_, events) -> events.any { it.id.isNotBlank() } }
        .keys.mapNotNull { runCatching { MapCoord.parse(it) }.getOrNull() }

    val density = LocalDensity.current
    val baseTileSizePx = with(density) { TILE_SIZE.toPx() }
    val wallColor = MaterialTheme.colorScheme.onSurface
    val colorEmpty = MaterialTheme.colorScheme.surfaceVariant
    val colorGrid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val colorDisabled = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val colorUnknown = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    val panOffsets = remember { mutableStateMapOf<String, Offset>() }
    val zoomScales = remember { mutableStateMapOf<String, Float>() }
    var panOffset by remember(mapData.id) { mutableStateOf(panOffsets[mapData.id] ?: Offset.Zero) }
    var zoomScale by remember(mapData.id) { mutableStateOf(zoomScales[mapData.id] ?: 1.0f) }
    var isDragging by remember { mutableStateOf(false) }
    val didDrag = remember { BooleanArray(1) }

    val cursor = if (isDragging) PointerIcon(Cursor(Cursor.MOVE_CURSOR)) else PointerIcon(Cursor(Cursor.HAND_CURSOR))

    Box(
        modifier = modifier
            .pointerHoverIcon(cursor)
            .pointerInput(mapData.id) {
                detectDragGestures(
                    onDragStart = { didDrag[0] = true; isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { _, delta ->
                    panOffset = panOffset + delta
                    panOffsets[mapData.id] = panOffset
                }
            }
            .pointerInput(mapData.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            zoomScale = (zoomScale - scrollY * ZOOM_STEP).coerceIn(ZOOM_MIN, ZOOM_MAX)
                            zoomScales[mapData.id] = zoomScale
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val tileSizePx = baseTileSizePx * zoomScale
        val tilesDp = TILE_SIZE * zoomScale

        Canvas(
            modifier = Modifier
                .size(tilesDp * grid.cols, tilesDp * grid.rows)
                .offset(
                    x = with(density) { panOffset.x.toDp() },
                    y = with(density) { panOffset.y.toDp() }
                )
                .pointerInput(mapData.id, grid.minX, grid.minY, grid.maxY, tileSizePx) {
                    detectTapGestures(
                        onPress = { didDrag[0] = false },
                        onTap = { offset ->
                            if (!didDrag[0]) {
                                val col = (offset.x / tileSizePx).toInt() + grid.minX
                                val row = grid.maxY - (offset.y / tileSizePx).toInt()
                                onSelectTile("$col,$row")
                            }
                        }
                    )
                }
        ) {
            drawGrid(grid, tileSizePx, colorEmpty, colorGrid)
            drawTiles(allCoords, mapData.tileProperties, selectedCoord, grid, tileSizePx, wallColor, colorDisabled, colorUnknown)
            drawSelection(selectedCoord, grid, tileSizePx)
            drawMarkers(troopCoords, eventCoords, grid, tileSizePx)
        }

        ChevronOverlay(grid.cols, grid.rows, panOffset, zoomScale, onExpand)
    }
}
