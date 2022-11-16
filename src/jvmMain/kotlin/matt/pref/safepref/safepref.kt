package matt.pref.safepref

import java.util.prefs.Preferences

class SafePref(val key: String) {
  private var cache: Preferences? = null
  private val prefs
	get() = cache ?: Preferences.userRoot().node(key).also { cache = it }

  val keys: Array<String>
	get() {
	  return try {
		prefs.keys()
	  } catch (e: IllegalStateException) {
		if ("Node has been removed" in (e.message ?: "")) {
		  val p = Preferences.userRoot().node(key)
		  cache = p
		  p.keys()
		} else {
		  throw e
		}
	  }
	}

  fun get(name: String?, default: String?) = prefs.get(name, default)
  fun put(name: String?, value: String) = prefs.put(name, value)

  fun getInt(name: String?, default: Int) = prefs.getInt(name, default)
  fun putInt(name: String?, value: Int) = prefs.putInt(name, value)

  fun getBoolean(name: String?, default: Boolean) = prefs.getBoolean(name, default)
  fun putBoolean(name: String?, value: Boolean) = prefs.putBoolean(name, value)

  fun removeNode() = prefs.removeNode()
  fun flush() = prefs.flush()
  fun remove(key: String) = prefs.remove(key)
}