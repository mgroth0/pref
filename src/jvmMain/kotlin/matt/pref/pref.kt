package matt.pref

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.prefs.Preferences
import kotlin.reflect.KProperty

abstract class PrefNodeBase {
  protected abstract fun string(defaultValue: String? = null): Any
  protected abstract fun int(defaultValue: Int? = null): Any
  protected abstract fun bool(defaultValue: Boolean? = null): Any
}

open class PrefNode(key: String, oldKeys: List<String>): PrefNodeBase() {
  private val prefs: Preferences by lazy { Preferences.userRoot().node(key) }

  init {
	oldKeys.forEach {
	  prefs.remove(it)
	}
  }

  protected inline fun <reified T: Any> obj(defaultValue: T? = null) =
	ObjPrefProvider(T::class.serializer(), defaultValue)

  override fun string(defaultValue: String?) = StringPrefProvider(defaultValue)
  override fun int(defaultValue: Int?) = IntPrefProvider(defaultValue)
  override fun bool(defaultValue: Boolean?) = BoolPrefProvider(defaultValue)
  abstract inner class Pref<T>(val name: String? = null, private val defaultValue: T? = null) {
	/*i use my own implementation of defaults because java's implementation seems confusing*/
	operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	): T? = if (name!! !in prefs.keys()) defaultValue else getFromNode()

	abstract fun getFromNode(): T?
	abstract fun putIntoNode(t: T)
	operator fun setValue(
	  thisRef: Any?, property: KProperty<*>, value: T?
	) {
	  if (value == null) {
		prefs.remove(name!!).also { println("removed pref ${prefs}..${name}") }
	  } else putIntoNode(value)
	}
  }

  inner class ObjPrefProvider<T: Any>(
	private val ser: KSerializer<T>, private val defaultValue: T? = null,
  ) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = ObjPref(ser = ser, defaultValue = defaultValue, name = prop.name)
  }

  inner class ObjPref<T: Any>(
	private val ser: KSerializer<T>, defaultValue: T? = null, name: String? = null
  ): Pref<T>(name = name, defaultValue = defaultValue) {
	override fun getFromNode() = Json.decodeFromString(ser, prefs.get(name, null))
	override fun putIntoNode(t: T) =
	  prefs.put(name!!, Json.encodeToString(ser, t)).also { println("set pref ${prefs}..${name}=${t}") }
  }

  inner class StringPrefProvider(private val defaultValue: String? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = StringPref(defaultValue = defaultValue, name = prop.name)
  }

  inner class StringPref(defaultValue: String? = null, name: String? = null):
	Pref<String>(name = name, defaultValue = defaultValue) {
	override fun getFromNode(): String? = prefs.get(name, null)
	override fun putIntoNode(t: String) = prefs.put(name!!, t)
  }

  inner class IntPrefProvider(private val defaultValue: Int? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = IntPref(defaultValue = defaultValue, name = prop.name)
  }

  inner class IntPref(defaultValue: Int? = null, name: String? = null):
	Pref<Int>(name = name, defaultValue = defaultValue) {
	override fun getFromNode() = prefs.getInt(name, 0)
	override fun putIntoNode(t: Int) = prefs.putInt(name!!, t)
  }

  inner class BoolPrefProvider(private val defaultValue: Boolean? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = BoolPref(defaultValue = defaultValue, name = prop.name)
  }

  inner class BoolPref(defaultValue: Boolean? = null, name: String? = null):
	Pref<Boolean>(name = name, defaultValue = defaultValue) {
	override fun getFromNode() = prefs.getBoolean(name, false)
	override fun putIntoNode(t: Boolean) = prefs.putBoolean(name!!, t)
  }
}

