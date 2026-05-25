import java.util.prefs.Preferences

object AppPrefs {
    private val prefs = Preferences.userRoot().node("fowl-jungle-editor")

    var lastProjectDir: String?
        get() = prefs.get("lastProjectDir", null)
        set(value) {
            if (value != null) prefs.put("lastProjectDir", value)
            else prefs.remove("lastProjectDir")
        }
}
