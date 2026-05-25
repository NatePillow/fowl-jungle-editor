package model

import kotlinx.serialization.Serializable

@Serializable
data class EquipmentItem(
    val id: String = "",
    val name: String = "",
    val goldValue: Int = 0,
    val slotType: String = "",
    val material: String = "",
    val damage: Int = 0,
    val protection: Int = 0,
    val levelRequirement: Int = 0,
    val damageTypes: Map<String, Double> = emptyMap(),
    val skillRequirements: Map<String, Int> = emptyMap()
)