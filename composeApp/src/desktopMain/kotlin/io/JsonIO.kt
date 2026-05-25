package io

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.EquipmentItem
import model.GameItem
import model.Recipe
import model.MapData
import model.Skill
import java.io.File

val jsonFormat = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

// Mirrors the fowl-jungle assets/json/ directory structure
object ProjectPaths {
    fun mapFiles(root: File): List<File> =
        root.resolve("world/maps").listFiles { f -> f.extension == "json" }?.toList() ?: emptyList()

    fun equipmentFiles(root: File): List<File> =
        root.resolve("equipment")
            .listFiles { f -> f.extension == "json" && !f.name.endsWith("_recipes.json") }
            ?.sortedBy { it.name } ?: emptyList()

    fun weaponFiles(root: File): List<File> =
        root.resolve("weapons")
            .listFiles { f -> f.extension == "json" && !f.name.endsWith("_recipes.json") }
            ?.sortedBy { it.name } ?: emptyList()

    fun itemFiles(root: File): List<File> =
        root.resolve("items")
            .listFiles { f -> f.extension == "json" && !f.name.endsWith("_recipes.json") && f.name != "loot_tables.json" }
            ?.sortedBy { it.name } ?: emptyList()

    fun recipeFiles(root: File): List<File> = buildList {
        root.resolve("weapons").listFiles { f -> f.name.endsWith("_recipes.json") }?.forEach { add(it) }
        root.resolve("items").listFiles { f -> f.name.endsWith("_recipes.json") }?.forEach { add(it) }
    }.sortedBy { it.name }

    fun skillsFile(root: File): File = root.resolve("lifeform/skills.json")
}

fun loadMaps(file: File): List<MapData> = jsonFormat.decodeFromString(file.readText())
fun saveMaps(file: File, maps: List<MapData>) = file.writeText(jsonFormat.encodeToString(maps))

fun loadEquipment(file: File): List<EquipmentItem> = jsonFormat.decodeFromString(file.readText())
fun loadItems(file: File): List<GameItem> = jsonFormat.decodeFromString(file.readText())
fun loadRecipes(file: File): List<Recipe> = jsonFormat.decodeFromString(file.readText())
fun loadSkills(file: File): List<Skill> = jsonFormat.decodeFromString(file.readText())
