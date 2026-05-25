import androidx.compose.runtime.*
import editors.map.DEFAULT_BOUNDARIES
import editors.map.DEFAULT_FPV_TEXTURES
import editors.map.DEFAULT_INGRESS
import editors.map.MapEditorState
import model.MapData
import model.TileProperties
import navigation.EditorDestination
import java.io.File

class AppState {
    var isDark by mutableStateOf(true)
    var selected by mutableStateOf<EditorDestination?>(null)
    val accordionOpen = mutableStateMapOf<EditorDestination, Boolean>()
    val selectedFileIndices = mutableStateMapOf<EditorDestination, Int>()
    val project = ProjectState()
    val mapEditorState = MapEditorState()
    val workingMaps = mutableStateMapOf<File, List<MapData>>()

    // New map dialog
    var showNewMapDialog by mutableStateOf(false)
    var newMapName by mutableStateOf("")
    var newMapWidth by mutableStateOf("10")
    var newMapHeight by mutableStateOf("10")
    var newMapFileName by mutableStateOf("")
    var newMapTileType by mutableStateOf("GRASS")
    var newMapMinimapTile by mutableStateOf("tiles/grass01.png")
    var newMapAutoBoundaries by mutableStateOf(false)
    var newMapBoundaryTexture by mutableStateOf("textures/stone_wall.jpg")

    // Rename dialog
    var showRenameDialog by mutableStateOf(false)
    var renameMapName by mutableStateOf("")
    var renameMapFileName by mutableStateOf("")

    // Delete dialog
    var showDeleteDialog by mutableStateOf(false)

    // Confirm dialogs
    var showReloadDialog by mutableStateOf(false)
    var showResetDialog by mutableStateOf(false)
    var showInitializeDialog by mutableStateOf(false)
    var showSettingsDialog by mutableStateOf(false)

    val selectedMapFileIndex get() = selectedFileIndices[EditorDestination.MapEditor]
    val mapEntry get() = selectedMapFileIndex?.let { project.mapFiles.getOrNull(it) }
    val currentFile get() = mapEntry?.first
    val currentMaps get() = mapEntry?.second ?: emptyList()

    fun saveMap() {
        val file = currentFile ?: return
        file.parentFile?.mkdirs()
        project.saveMapFile(file, mapEditorState.maps)
        workingMaps.remove(file)
        mapEditorState.dirty = false
    }

    fun resetMap() {
        val file = currentFile ?: return
        workingMaps.remove(file)
        mapEditorState.load(project.reloadMapFile(file))
    }

    fun createMap() {
        val root = project.root ?: return
        val w = newMapWidth.toIntOrNull()?.coerceAtLeast(1) ?: 10
        val h = newMapHeight.toIntOrNull()?.coerceAtLeast(1) ?: 10
        val rawFileName = newMapFileName.trim().let { if (it.endsWith(".json", ignoreCase = true)) it else "$it.json" }
        val mapId = rawFileName.removeSuffix(".json")
        val tiles = buildMap {
            for (x in 1..w) for (y in 1..h) {
                val edgeDirs = if (newMapAutoBoundaries) buildList {
                    if (y == h) add("NORTH")
                    if (y == 1) add("SOUTH")
                    if (x == w) add("EAST")
                    if (x == 1) add("WEST")
                } else emptyList()
                val directions = listOf("NORTH", "SOUTH", "EAST", "WEST")
                val drawBoundaries = directions.associateWith { if (it in edgeDirs) "true" else "false" }
                val ingress = directions.associateWith { if (it in edgeDirs) "false" else "true" }
                val fpvTextures = if (edgeDirs.isNotEmpty())
                    DEFAULT_FPV_TEXTURES + edgeDirs.associate { "${it}_BOUNDARY" to listOf(newMapBoundaryTexture) }
                else DEFAULT_FPV_TEXTURES
                put("$x,$y", TileProperties(
                    type = newMapTileType,
                    minimapTile = newMapMinimapTile,
                    ingress = ingress,
                    drawBoundaries = drawBoundaries,
                    fpvTextures = fpvTextures
                ))
            }
        }
        val file = root.resolve("world/maps/$rawFileName")
        project.addPendingMapFile(file, listOf(MapData(id = mapId, name = newMapName.trim(), tileProperties = tiles)))
        selectedFileIndices[EditorDestination.MapEditor] = project.mapFiles.size - 1
        accordionOpen[EditorDestination.MapEditor] = true
        selected = EditorDestination.MapEditor
        dismissNewMapDialog()
    }

    fun dismissNewMapDialog() {
        showNewMapDialog = false
        newMapName = ""
        newMapFileName = ""
        newMapWidth = "10"
        newMapHeight = "10"
        newMapTileType = "GRASS"
        newMapMinimapTile = "tiles/grass01.png"
        newMapAutoBoundaries = false
        newMapBoundaryTexture = "textures/stone_wall.jpg"
    }

    fun openRenameDialog() {
        val map = mapEditorState.maps.getOrNull(mapEditorState.selectedMapIndex)
        renameMapFileName = currentFile?.name ?: ""
        renameMapName = map?.name?.takeIf { it.isNotBlank() } ?: (currentFile?.nameWithoutExtension ?: "")
        showRenameDialog = true
    }

    fun dismissRenameDialog() {
        showRenameDialog = false
        renameMapName = ""
        renameMapFileName = ""
    }

    fun renameMap() {
        val file = currentFile ?: return
        val newFileName = renameMapFileName.trim().let {
            if (it.endsWith(".json", ignoreCase = true)) it else "$it.json"
        }
        val newFile = file.parentFile?.resolve(newFileName) ?: return

        // Update name on selected map
        val idx = mapEditorState.selectedMapIndex
        val updatedMaps = mapEditorState.maps.mapIndexed { i, m ->
            if (i == idx) m.copy(name = renameMapName.trim()) else m
        }

        // Rename file on disk if path changed
        if (newFile != file && file.exists()) file.renameTo(newFile)
        workingMaps.remove(file)
        workingMaps.remove(newFile)

        // Save atomically — rename is only available when clean
        newFile.parentFile?.mkdirs()
        project.renameMapFile(file, newFile, updatedMaps)
        project.saveMapFile(newFile, updatedMaps)

        mapEditorState.maps = updatedMaps
        mapEditorState.dirty = false

        dismissRenameDialog()
    }

    fun deleteMap() {
        val file = currentFile ?: return
        val idx = selectedMapFileIndex ?: return

        if (file.exists()) file.delete()
        workingMaps.remove(file)
        project.deleteMapFile(file)

        val newSize = project.mapFiles.size
        if (newSize == 0) {
            selectedFileIndices.remove(EditorDestination.MapEditor)
        } else {
            selectedFileIndices[EditorDestination.MapEditor] = idx.coerceAtMost(newSize - 1)
        }
        showDeleteDialog = false
    }
    fun initialize() {
        AppPrefs.lastProjectDir = null
        isDark = true
        selected = null
        accordionOpen.clear()
        selectedFileIndices.clear()
        workingMaps.clear()
        project.close()
        mapEditorState.reset()
        showInitializeDialog = false
        showSettingsDialog = false
        dismissNewMapDialog()
        dismissRenameDialog()
        showDeleteDialog = false
        showReloadDialog = false
        showResetDialog = false
        restoreSample()
        defaultProjectDir()?.let { project.open(it) }
    }
}

private val SAMPLE_RESOURCES = listOf(
    "sample/json/world/maps/test_map.json",
    "sample/json/lifeform/skills.json"
)

private fun restoreSample() {
    val base = listOf(File("."), File(".."))
        .firstOrNull { File(it, "sample").isDirectory } ?: return
    for (path in SAMPLE_RESOURCES) {
        val resource = object {}::class.java.classLoader.getResourceAsStream(path) ?: continue
        val target = File(base, path)
        target.parentFile?.mkdirs()
        target.writeBytes(resource.readBytes())
    }
}

private fun defaultProjectDir(): File? =
    listOf(File("sample/json"), File("../sample/json"))
        .firstOrNull { it.exists() && it.isDirectory }

@Composable
fun rememberAppState(): AppState {
    val state = remember { AppState() }

    LaunchedEffect(Unit) {
        val saved = AppPrefs.lastProjectDir?.let { File(it) }?.takeIf { it.exists() && it.isDirectory }
        val dir = saved ?: defaultProjectDir()
        dir?.let { state.project.open(it) }
    }

    // Clear selection only when the project root changes, not on every mapFiles mutation
    LaunchedEffect(state.project.root) {
        state.selectedFileIndices.clear()
    }

    LaunchedEffect(state.currentFile) {
        val working = state.currentFile?.let { state.workingMaps[it] }
        state.mapEditorState.load(working ?: state.currentMaps)
        // Pending (not-yet-saved) maps and maps with unsaved changes are both dirty
        if (working != null || state.currentFile?.exists() == false) state.mapEditorState.dirty = true
    }

    LaunchedEffect(state.mapEditorState.maps, state.mapEditorState.dirty) {
        val file = state.currentFile
        if (file != null && state.mapEditorState.dirty) {
            state.workingMaps[file] = state.mapEditorState.maps
        }
    }

    return state
}
