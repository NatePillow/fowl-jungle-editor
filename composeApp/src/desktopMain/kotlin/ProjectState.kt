import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ProjectPaths
import io.loadEquipment
import io.loadItems
import io.loadMaps
import io.loadRecipes
import io.loadSkills
import io.saveMaps
import model.EquipmentItem
import model.GameItem
import model.Recipe
import model.MapData
import java.io.File


class ProjectState {
    var root by mutableStateOf<File?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    // Per-editor data — populated when a project is opened
    var mapFiles by mutableStateOf<List<Pair<File, List<MapData>>>>(emptyList())
    var equipmentFiles by mutableStateOf<List<Pair<File, List<EquipmentItem>>>>(emptyList())
    var weaponFiles by mutableStateOf<List<Pair<File, List<EquipmentItem>>>>(emptyList())
    var itemFiles by mutableStateOf<List<Pair<File, List<GameItem>>>>(emptyList())
    var recipeFiles by mutableStateOf<List<Pair<File, List<Recipe>>>>(emptyList())
    var tileImageFiles by mutableStateOf<List<String>>(emptyList())
    var textureImageFiles by mutableStateOf<List<String>>(emptyList())
    var skillNames by mutableStateOf<Map<String, String>>(emptyMap())

    val itemNames get() = itemFiles.flatMap { (_, items) -> items }.associate { it.id to it.name }

    val isOpen get() = root != null

    fun open(dir: File) {
        try {
            root = dir
            errorMessage = null
            mapFiles = ProjectPaths.mapFiles(dir).map { file -> file to loadMaps(file) }
            equipmentFiles = ProjectPaths.equipmentFiles(dir).map { file -> file to loadEquipment(file) }
            weaponFiles = ProjectPaths.weaponFiles(dir).map { file -> file to loadEquipment(file) }
            itemFiles = ProjectPaths.itemFiles(dir).map { file -> file to loadItems(file) }
            recipeFiles = ProjectPaths.recipeFiles(dir).map { file -> file to loadRecipes(file) }
            tileImageFiles = dir.parentFile
                ?.resolve("tiles")
                ?.listFiles { _, name -> name.endsWith(".png", ignoreCase = true) }
                ?.map { "tiles/${it.name}" }?.sorted() ?: emptyList()
            textureImageFiles = dir.parentFile
                ?.resolve("textures")
                ?.listFiles { _, name -> name.endsWith(".png", ignoreCase = true) || name.endsWith(".jpg", ignoreCase = true) }
                ?.map { "textures/${it.name}" }?.sorted() ?: emptyList()
            skillNames = loadSkills(ProjectPaths.skillsFile(dir)).associate { it.id to it.name }
            AppPrefs.lastProjectDir = dir.absolutePath
        } catch (e: Exception) {
            errorMessage = "Failed to open project: ${e.message}"
        }
    }

    fun saveMapFile(file: File, maps: List<MapData>) {
        saveMaps(file, maps)
        mapFiles = mapFiles.map { (f, m) -> if (f == file) f to maps else f to m }
    }

    fun createMapFile(file: File, maps: List<MapData>) {
        file.parentFile?.mkdirs()
        saveMaps(file, maps)
        mapFiles = mapFiles + (file to maps)
    }

    fun reloadMapFile(file: File): List<MapData> {
        val fresh = loadMaps(file)
        mapFiles = mapFiles.map { (f, m) -> if (f == file) f to fresh else f to m }
        return fresh
    }

    fun addPendingMapFile(file: File, maps: List<MapData>) {
        mapFiles = mapFiles + (file to maps)
    }

    fun renameMapFile(oldFile: File, newFile: File, maps: List<MapData>) {
        mapFiles = mapFiles.map { (f, m) -> if (f == oldFile) newFile to maps else f to m }
    }

    fun deleteMapFile(file: File) {
        mapFiles = mapFiles.filter { (f, _) -> f != file }
    }

    fun close() {
        root = null
        errorMessage = null
        mapFiles = emptyList()
        equipmentFiles = emptyList()
        weaponFiles = emptyList()
        itemFiles = emptyList()
        recipeFiles = emptyList()
        tileImageFiles = emptyList()
        textureImageFiles = emptyList()
        skillNames = emptyMap()
    }
}
