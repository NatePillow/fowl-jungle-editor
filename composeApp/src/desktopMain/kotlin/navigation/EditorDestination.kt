package navigation

sealed class EditorDestination(val label: String, val enabled: Boolean) {
    data object World : EditorDestination("World", false)
    data object MapEditor : EditorDestination("Map", true)
    data object Battle : EditorDestination("Battle", false)
    data object Troops : EditorDestination("Troops", false)
    data object Lifeforms : EditorDestination("Lifeforms", false)
    data object Equipment : EditorDestination("Equipment", true)
    data object Weapons : EditorDestination("Weapons", true)
    data object Items : EditorDestination("Items", true)
    data object Recipes : EditorDestination("Recipes", true)

    companion object {
        val all: List<EditorDestination> by lazy {
            listOf(World, MapEditor, Battle, Troops, Lifeforms, Equipment, Weapons, Items, Recipes)
        }
    }
}
