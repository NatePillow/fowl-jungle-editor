package editors.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import model.MapBounds
import model.MapCoord
import model.MapData
import model.TileProperties

val DEFAULT_INGRESS = mapOf("NORTH" to "true", "EAST" to "true", "SOUTH" to "true", "WEST" to "true")
val DEFAULT_BOUNDARIES = mapOf("NORTH" to "false", "EAST" to "false", "SOUTH" to "false", "WEST" to "false")
val DEFAULT_FPV_TEXTURES = mapOf(
    "NORTH_BOUNDARY" to emptyList<String>(),
    "EAST_BOUNDARY" to emptyList(),
    "SOUTH_BOUNDARY" to emptyList(),
    "WEST_BOUNDARY" to emptyList(),
    "CEILING" to emptyList(),
    "FLOOR" to emptyList(),
    "CLOSE_FOREGROUND" to emptyList(),
    "FOREGROUND" to emptyList(),
    "FAR_FOREGROUND" to emptyList()
)

class MapEditorState {
    var maps by mutableStateOf<List<MapData>>(emptyList())
    var selectedMapIndex by mutableStateOf(0)
    var selectedCoord by mutableStateOf<String?>(null)
    var dirty by mutableStateOf(false)
    var autoAddBoundaries by mutableStateOf(false)
    var autoAddTiles by mutableStateOf(false)
    var autoUpdateAdjacentTiles by mutableStateOf(false)
    var hideLegend by mutableStateOf(false)

    val currentMap: MapData? get() = maps.getOrNull(selectedMapIndex)
    val selectedTile: TileProperties? get() = selectedCoord?.let { currentMap?.tileProperties?.get(it) }

    fun load(maps: List<MapData>) {
        this.maps = maps
        selectedMapIndex = 0
        selectedCoord = null
        dirty = false
    }

    fun reset() {
        maps = emptyList()
        selectedMapIndex = 0
        selectedCoord = null
        dirty = false
        autoAddBoundaries = false
        autoAddTiles = false
        autoUpdateAdjacentTiles = false
        hideLegend = false
    }

    fun updateTile(coord: String, tile: TileProperties) {
        val map = maps.getOrNull(selectedMapIndex) ?: return
        maps = maps.toMutableList().also {
            it[selectedMapIndex] = map.copy(tileProperties = map.tileProperties + (coord to tile))
        }
        dirty = true
    }

    fun addTile(coord: String) {
        val map = maps.getOrNull(selectedMapIndex) ?: return
        if (map.tileProperties.containsKey(coord)) return

        val mc = runCatching { MapCoord.parse(coord) }.getOrNull()
        val (boundaries, ingress) = buildIngressAndBoundaries(map.tileProperties, mc)
        val fpvTextures = buildFpvTextures(map.tileProperties, mc, boundaries)
        val (minimapTile, tileType) = buildTileAppearance(map.tileProperties, mc)

        val tile = TileProperties(
            type = tileType,
            minimapTile = minimapTile,
            ingress = ingress,
            drawBoundaries = boundaries,
            fpvTextures = fpvTextures
        )

        val updatedProps = if (autoUpdateAdjacentTiles && mc != null)
            updateAdjacentTiles(map.tileProperties + (coord to tile), mc)
        else
            map.tileProperties + (coord to tile)

        maps = maps.toMutableList().also {
            it[selectedMapIndex] = map.copy(tileProperties = updatedProps)
        }
        dirty = true
    }

    private fun buildIngressAndBoundaries(
        tileProperties: Map<String, TileProperties>,
        mc: MapCoord?
    ): Pair<Map<String, String>, Map<String, String>> {
        if (!autoAddBoundaries || mc == null) return DEFAULT_BOUNDARIES to DEFAULT_INGRESS
        val neighbors = mapOf(
            "NORTH" to "${mc.x},${mc.y + 1}",
            "SOUTH" to "${mc.x},${mc.y - 1}",
            "EAST"  to "${mc.x + 1},${mc.y}",
            "WEST"  to "${mc.x - 1},${mc.y}"
        )
        val boundaries = neighbors.mapValues { (_, n) -> if (tileProperties.containsKey(n)) "false" else "true" }
        val ingress = boundaries.mapValues { (_, v) -> if (v == "true") "false" else "true" }
        return boundaries to ingress
    }

    private fun buildFpvTextures(
        tileProperties: Map<String, TileProperties>,
        mc: MapCoord?,
        boundaries: Map<String, String>
    ): Map<String, List<String>> {
        val fpv = DEFAULT_FPV_TEXTURES.toMutableMap()
        if (autoAddBoundaries && mc != null) {
            val texture = findBoundaryTexture(tileProperties, mc)
            if (texture != null)
                boundaries.forEach { (dir, v) -> if (v == "true") fpv["${dir}_BOUNDARY"] = listOf(texture) }
        }
        if (autoAddTiles && mc != null) {
            val result = findAutoTileTextures(tileProperties, mc)
            if (result.ceiling != null)    fpv["CEILING"] = listOf(result.ceiling)
            if (result.floor != null)      fpv["FLOOR"]   = listOf(result.floor)
        }
        return fpv
    }

    private fun buildTileAppearance(
        tileProperties: Map<String, TileProperties>,
        mc: MapCoord?
    ): Pair<String, String> {
        if (!autoAddTiles || mc == null) return "tiles/grass01.png" to "GRASS"
        val result = findAutoTileTextures(tileProperties, mc)
        return (result.minimapTile ?: "tiles/grass01.png") to (result.type ?: "GRASS")
    }

    private fun updateAdjacentTiles(
        props: Map<String, TileProperties>,
        mc: MapCoord
    ): Map<String, TileProperties> {
        val opposite = mapOf("NORTH" to "SOUTH", "SOUTH" to "NORTH", "EAST" to "WEST", "WEST" to "EAST")
        val neighbors = mapOf(
            "NORTH" to "${mc.x},${mc.y + 1}",
            "SOUTH" to "${mc.x},${mc.y - 1}",
            "EAST"  to "${mc.x + 1},${mc.y}",
            "WEST"  to "${mc.x - 1},${mc.y}"
        )
        var updated = props
        for ((dir, neighborCoord) in neighbors) {
            val neighborSide = opposite[dir] ?: continue
            val neighbor = updated[neighborCoord] ?: continue
            if (neighbor.drawBoundaries[neighborSide]?.lowercase() == "true") {
                updated = updated + (neighborCoord to neighbor.copy(
                    drawBoundaries = neighbor.drawBoundaries + (neighborSide to "false"),
                    ingress = neighbor.ingress + (neighborSide to "true")
                ))
            }
        }
        return updated
    }

    fun removeTile(coord: String) {
        val map = maps.getOrNull(selectedMapIndex) ?: return
        maps = maps.toMutableList().also {
            it[selectedMapIndex] = map.copy(tileProperties = map.tileProperties - coord)
        }
        if (selectedCoord == coord) selectedCoord = null
        dirty = true
    }

    fun expand(direction: ExpandDirection) {
        val map = maps.getOrNull(selectedMapIndex) ?: return
        val coords = map.tileProperties.keys.mapNotNull { runCatching { MapCoord.parse(it) }.getOrNull() }
        if (coords.isEmpty()) return
        val current = map.bounds ?: MapBounds(
            minX = coords.minOf { it.x },
            minY = coords.minOf { it.y },
            maxX = coords.maxOf { it.x },
            maxY = coords.maxOf { it.y }
        )
        val newBounds = when (direction) {
            ExpandDirection.North -> current.copy(maxY = current.maxY + 1)
            ExpandDirection.South -> current.copy(minY = current.minY - 1)
            ExpandDirection.East  -> current.copy(maxX = current.maxX + 1)
            ExpandDirection.West  -> current.copy(minX = current.minX - 1)
        }
        maps = maps.toMutableList().also {
            it[selectedMapIndex] = map.copy(bounds = newBounds)
        }
        dirty = true
    }
}

enum class ExpandDirection { North, South, East, West }

private val BOUNDARY_FPV_SLOTS = listOf("NORTH_BOUNDARY", "SOUTH_BOUNDARY", "EAST_BOUNDARY", "WEST_BOUNDARY")

private fun List<String>.mostCommon(): String? =
    if (isEmpty()) null
    else groupingBy { it }.eachCount()
        .entries.maxWithOrNull(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })?.key

private fun neighborsOf(mc: MapCoord) = listOf(
    MapCoord(mc.x, mc.y + 1),
    MapCoord(mc.x, mc.y - 1),
    MapCoord(mc.x + 1, mc.y),
    MapCoord(mc.x - 1, mc.y)
)

private fun bfsFrontiers(tileProperties: Map<String, TileProperties>, startCoord: MapCoord): Sequence<List<MapCoord>> = sequence {
    val visited = mutableSetOf(startCoord.toString())
    var frontier = neighborsOf(startCoord)
        .filter { tileProperties.containsKey(it.toString()) && visited.add(it.toString()) }
    while (frontier.isNotEmpty()) {
        yield(frontier)
        frontier = frontier.flatMap { mc ->
            neighborsOf(mc).filter { n -> tileProperties.containsKey(n.toString()) && visited.add(n.toString()) }
        }
    }
}

private fun findBoundaryTexture(tileProperties: Map<String, TileProperties>, startCoord: MapCoord): String? {
    for (frontier in bfsFrontiers(tileProperties, startCoord)) {
        val textures = frontier.flatMap { mc ->
            val tile = tileProperties[mc.toString()] ?: return@flatMap emptyList()
            BOUNDARY_FPV_SLOTS.mapNotNull { slot -> tile.fpvTextures[slot]?.firstOrNull()?.takeIf { it.isNotBlank() } }
        }
        if (textures.isNotEmpty()) return textures.mostCommon()
    }
    return null
}

data class AutoTileResult(val ceiling: String?, val floor: String?, val minimapTile: String?, val type: String?)

private fun findAutoTileTextures(tileProperties: Map<String, TileProperties>, startCoord: MapCoord): AutoTileResult {
    for (frontier in bfsFrontiers(tileProperties, startCoord)) {
        val ceilings  = mutableListOf<String>()
        val floors    = mutableListOf<String>()
        val minimaps  = mutableListOf<String>()
        val types     = mutableListOf<String>()
        for (mc in frontier) {
            val tile = tileProperties[mc.toString()] ?: continue
            tile.fpvTextures["CEILING"]?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { ceilings.add(it) }
            tile.fpvTextures["FLOOR"]?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { floors.add(it) }
            tile.minimapTile.takeIf { it.isNotBlank() }?.let { minimaps.add(it) }
            tile.type.takeIf { it.isNotBlank() }?.let { types.add(it) }
        }
        if (ceilings.isNotEmpty() || floors.isNotEmpty() || minimaps.isNotEmpty() || types.isNotEmpty())
            return AutoTileResult(ceilings.mostCommon(), floors.mostCommon(), minimaps.mostCommon(), types.mostCommon())
    }
    return AutoTileResult(null, null, null, null)
}

