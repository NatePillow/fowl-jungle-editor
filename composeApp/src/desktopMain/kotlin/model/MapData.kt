package model

import kotlinx.serialization.Serializable

@Serializable
data class MapBounds(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

@Serializable
data class MapData(
    val id: String,
    val name: String = "",
    val bounds: MapBounds? = null,
    val startingMap: Boolean = false,
    val defaultSpawnPosition: String = "1,1",
    val tileProperties: Map<String, TileProperties> = emptyMap(),
    val troops: Map<String, List<TroopSpawn>> = emptyMap(),
    val events: Map<String, List<MapEvent>> = emptyMap(),
    val temperature: String = "TEMPERATE",
    val weather: List<WeatherEntry> = emptyList(),
    val minimumWeatherSteps: Int = 90,
    val weatherChangeProbability: Double = 0.05
)

@Serializable
data class TileProperties(
    val disabled: Boolean = false,
    val type: String = "GRASS",
    val minimapTile: String = "",
    val minimapIcon: String = "",
    val ingress: Map<String, String> = emptyMap(),
    val drawBoundaries: Map<String, String> = emptyMap(),
    val fpvTextures: Map<String, List<String>> = emptyMap()
)

@Serializable
data class TroopSpawn(val id: String)

@Serializable
data class MapEvent(
    val id: String,
    val sprite: String = "",
    val spriteType: String = "STATIC",
    val triggeredSprite: String = "",
    val triggeredSpriteType: String = "STATIC",
    val eventType: String = "",
    val triggerType: String = "",
    val lootTable: String = ""
)

@Serializable
data class WeatherEntry(
    val probability: Double,
    val types: List<String> = emptyList()
)

data class MapCoord(val x: Int, val y: Int) {
    override fun toString() = "$x,$y"

    companion object {
        fun parse(s: String): MapCoord {
            val parts = s.split(",")
            return MapCoord(parts[0].trim().toInt(), parts[1].trim().toInt())
        }
    }
}
