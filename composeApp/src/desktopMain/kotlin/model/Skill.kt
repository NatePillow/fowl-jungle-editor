package model

import kotlinx.serialization.Serializable

@Serializable
data class Skill(
    val id: String = "",
    val name: String = ""
)
