import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import editors.equipment.EquipmentEditor
import editors.items.ItemsEditor
import editors.map.MapEditor
import editors.map.DeleteMapDialog
import editors.map.MapMenuActions
import editors.map.NewMapDialog
import editors.map.RenameMapDialog
import editors.map.ResetMapDialog
import ui.InitializeDialog
import ui.ReloadConfirmDialog
import ui.SettingsDialog
import editors.recipes.RecipesEditor
import editors.weapons.WeaponsEditor
import navigation.EditorDestination
import ui.AccordionDivider
import ui.AccordionItem
import ui.AppMenuBar
import ui.SidebarButton

private fun String.toTitleCase() =
    split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

@Composable
fun App() {
    val state = rememberAppState()
    MaterialTheme(colorScheme = if (state.isDark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.key == Key.Escape && event.type == KeyEventType.KeyDown && !state.showSettingsDialog) {
                state.showSettingsDialog = true; true
            } else false
        }) {
            Column {
                AppMenuBar(
                    state = state,
                    editorActions = when {
                        state.selected == EditorDestination.MapEditor && state.project.isOpen -> {
                            { MapMenuActions(state) }
                        }
                        else -> null
                    }
                )
                HorizontalDivider()
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AppSidebar(state)
                    VerticalDivider()
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        AppContent(state)
                    }
                }
            }
            AppDialogs(state)
        }
    }
}

@Composable
private fun AppDialogs(state: AppState) {
    if (state.showNewMapDialog)      NewMapDialog(state)
    if (state.showRenameDialog)      RenameMapDialog(state)
    if (state.showDeleteDialog)      DeleteMapDialog(state)
    if (state.showReloadDialog)      ReloadConfirmDialog(state)
    if (state.showResetDialog)       ResetMapDialog(state)
    if (state.showSettingsDialog)    SettingsDialog(state)
    if (state.showInitializeDialog)  InitializeDialog(state)
}

@Composable
private fun AppSidebar(state: AppState) {
    Column(modifier = Modifier.width(180.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(52.dp))

        EditorDestination.all.forEach { dest ->
            val isSelected = state.selected == dest
            val accordionLabels: List<String> = when (dest) {
                EditorDestination.MapEditor -> state.project.mapFiles.map { it.first.nameWithoutExtension.toTitleCase() }
                EditorDestination.Equipment -> state.project.equipmentFiles.map { it.first.nameWithoutExtension.toTitleCase() }
                EditorDestination.Weapons -> state.project.weaponFiles.map { it.first.nameWithoutExtension.toTitleCase() }
                EditorDestination.Items -> state.project.itemFiles.map { it.first.nameWithoutExtension.toTitleCase() }
                EditorDestination.Recipes -> state.project.recipeFiles.map { it.first.nameWithoutExtension.toTitleCase() }
                else -> emptyList()
            }
            val hasAccordion = accordionLabels.isNotEmpty()
            val enabled = dest.enabled && accordionLabels.isNotEmpty()

            SidebarButton(
                label = dest.label,
                isSelected = isSelected,
                enabled = enabled,
                hasAccordion = hasAccordion,
                accordionOpen = state.accordionOpen[dest] == true,
                onClick = {
                    if (!enabled) return@SidebarButton
                    state.selected = dest
                    if (hasAccordion) {
                        val opening = !(state.accordionOpen[dest] ?: false)
                        state.accordionOpen[dest] = opening
                        if (!opening) state.selectedFileIndices.remove(dest)
                    }
                }
            )

            if (hasAccordion && state.accordionOpen[dest] == true) {
                val selectedIndex = state.selectedFileIndices[dest]
                accordionLabels.forEachIndexed { index, label ->
                    AccordionItem(
                        label = label,
                        isSelected = selectedIndex != null && index == selectedIndex,
                        onClick = { state.selected = dest; state.selectedFileIndices[dest] = index }
                    )
                }
                AccordionDivider()
            }
        }

        Spacer(Modifier.height(52.dp))
    }
}

@Composable
private fun AppContent(state: AppState) {
    if (!state.project.isOpen) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Open a project folder to begin")
        }
        return
    }
    val selected = state.selected
    if (selected == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a section from the navigation sidebar")
        }
        return
    }
    val selIdx = state.selectedFileIndices[selected]
    if (selIdx == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a file from the navigation sidebar")
        }
        return
    }
    when (selected) {
        EditorDestination.MapEditor -> MapEditor(
            state = state.mapEditorState,
            tileImageFiles = state.project.tileImageFiles,
            textureImageFiles = state.project.textureImageFiles,
            canvasLabel = state.currentFile?.let {
                if (state.mapEditorState.dirty) {
                    if (!it.exists()) "${it.nameWithoutExtension} (new)" else "${it.nameWithoutExtension} *"
                } else it.nameWithoutExtension
            },
        )
        EditorDestination.Equipment -> EquipmentEditor(
            items = state.project.equipmentFiles.getOrNull(selIdx)?.second ?: emptyList(),
            skillNames = state.project.skillNames
        )
        EditorDestination.Weapons -> WeaponsEditor(
            items = state.project.weaponFiles.getOrNull(selIdx)?.second ?: emptyList(),
            skillNames = state.project.skillNames
        )
        EditorDestination.Items -> ItemsEditor(
            items = state.project.itemFiles.getOrNull(selIdx)?.second ?: emptyList()
        )
        EditorDestination.Recipes -> RecipesEditor(
            recipes = state.project.recipeFiles.getOrNull(selIdx)?.second ?: emptyList(),
            skillNames = state.project.skillNames,
            itemNames = state.project.itemNames
        )
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("${selected.label} — coming soon")
        }
    }
}
