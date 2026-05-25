package model

import kotlinx.serialization.Serializable

@Serializable
data class GameItem(
    val id: String = "",
    val name: String = "",
    val goldValue: Int = 0,
    val icon: String = "",
    val craftingItemType: String = ""
)

@Serializable
data class Recipe(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val ticks: Int = 0,
    val skillRequirements: Map<String, Int> = emptyMap(),
    val itemRequirements: Map<String, Int> = emptyMap(),
    val result: Map<String, Int> = emptyMap()
)
