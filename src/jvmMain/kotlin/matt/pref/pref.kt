@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package matt.pref

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.prefs.Preferences
import kotlin.reflect.KProperty

abstract class PrefNodeBase {
  protected abstract fun string(defaultValue: String? = null): Any
  protected abstract fun int(defaultValue: Int? = null): Any
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

  inner class ObjPrefProvider<T: Any>(
	private val ser: KSerializer<T>, private val defaultValue: T? = null,
  ) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	): ObjPref<T> {
	  return ObjPref(ser = ser, defaultValue = defaultValue, name = prop.name)
	}
  }

  @OptIn(InternalSerializationApi::class) inner class ObjPref<T: Any>(
	private val ser: KSerializer<T>, private val defaultValue: T? = null, val name: String? = null
  ) {

	/*i use my own implementation of defaults because java's implementation seems confusing*/
	operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	): T? = if (name!! !in prefs.keys()) defaultValue else Json.decodeFromString(
	  ser, prefs.get(name, null).also { println("got pref ${prefs}..${name}=${it}") }
	)


	operator fun setValue(
	  thisRef: Any?, property: KProperty<*>, value: T?
	) {
	  if (value == null) {
		prefs.remove(name!!).also { println("removed pref ${prefs}..${name}") }
	  } else prefs.put(name!!, Json.encodeToString(ser, value))
		.also { println("set pref ${prefs}..${name}=${value}") }
	}
  }

  inner class StringPrefProvider(private val defaultValue: String? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	): StringPref {
	  return StringPref(defaultValue = defaultValue, name = prop.name)
	}

  }

  inner class StringPref(private val defaultValue: String? = null, val name: String? = null) {


	/*i use my own implementation of defaults because java's implementation seems confusing*/
	operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	): String? = if (name!! !in prefs.keys()) defaultValue else prefs.get(name, null)


	operator fun setValue(
	  thisRef: Any?, property: KProperty<*>, value: String?
	) {
	  if (value == null) {
		prefs.remove(name!!)
	  } else prefs.put(name!!, value)
	}
  }

  inner class IntPrefProvider(private val defaultValue: Int? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	): IntPref {
	  return IntPref(defaultValue = defaultValue, name = prop.name)
	}

  }

  inner class IntPref(private val defaultValue: Int? = null, val name: String? = null) {


	/*i use my own implementation of defaults because java's implementation seems confusing*/
	operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	) = if (name!! !in prefs.keys()) defaultValue else prefs.getInt(name, 0)


	operator fun setValue(
	  thisRef: Any?, property: KProperty<*>, value: Int?
	) {
	  if (value == null) {
		prefs.remove(name!!)
	  } else prefs.putInt(name!!, value)
	}
  }


}

