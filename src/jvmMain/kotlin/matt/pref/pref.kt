package matt.pref

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import matt.lang.delegation.provider
import matt.log.warn.warn
import matt.model.code.errreport.printReport
import matt.model.obj.del.Deletable
import matt.pref.safepref.SafePref
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class PrefNodeBase: Deletable {
  protected abstract fun string(defaultValue: String? = null): Any
  protected abstract fun int(defaultValue: Int? = null): Any
  protected abstract fun bool(defaultValue: Boolean? = null): Any
}

open class PrefNode(private val key: String, oldKeys: List<String>, val json: Json = Json): PrefNodeBase() {


  private val prefs = SafePref(key)

  //  private val myStr by lazy {
  //	toStringBuilder(
  //	  "key" to key
  //	)
  //  }

  override fun toString() = "${this::class.simpleName}[key=$key]"

  override fun delete() {
	prefs.removeNode()
	prefs.flush()
	println("deleted $this")
  }

  init {
	oldKeys.forEach {
	  prefs.remove(it)
	}
  }

  protected inline fun <reified T: Any> obj(defaultValue: T? = null) =
	ObjPrefProvider(T::class.serializer(), defaultValue)

  override fun string(defaultValue: String?) = provider { StringPref(defaultValue, it) }
  override fun int(defaultValue: Int?) = provider { IntPref(defaultValue, it) }
  override fun bool(defaultValue: Boolean?) = provider { BoolPref(defaultValue, it) }


  abstract inner class Pref<T>(val name: String? = null, protected val defaultValue: T? = null):
	  ReadWriteProperty<Any?, T?> {
	/*i use my own implementation of defaults because java's implementation seems confusing*/
	override operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	) = if (name!! !in prefs.keys) defaultValue else getFromNode()

	abstract fun getFromNode(): T?
	abstract fun putIntoNode(t: T)
	override operator fun setValue(
	  thisRef: Any?, property: KProperty<*>, value: T?
	) {
	  if (value == null) {
		prefs.remove(name!!).also { println("removed pref ${prefs}..${name}") }
	  } else putIntoNode(value)
	  prefs.flush()
	}
  }

  inner class ObsObjPref<T: Any>(
	private val ser: KSerializer<T>, val defaultValue: ()->T, val name: String? = null
  ) {

	fun get() = if (name!! !in prefs.keys) {
	  defaultValue()
	} else getFromNode()

	operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	): T = get()


	fun getFromNode() = try {
	  json.decodeFromString(ser, prefs.get(name, null))
	} catch (e: SerializationException) {
	  warn("got $e when trying to load preference node \"$name\"")
	  e.printReport()
	  defaultValue()
	}

	fun putIntoNode(t: T, silent: Boolean = false) =
	  prefs.put(name!!, json.encodeToString(ser, t)).also { if (!silent) println("set pref ${prefs}..${name}=${t}") }
  }

  inner class ObjPrefProvider<T: Any>(
	private val ser: KSerializer<T>, private val defaultValue: T? = null,
  ) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = ObjPref(ser = ser, defaultValue = defaultValue, name = prop.name, silent = false)
  }

  inner class ObjPref<T: Any>(
	private val ser: KSerializer<T>, defaultValue: T? = null, name: String? = null, val silent: Boolean
  ): Pref<T>(name = name, defaultValue = defaultValue) {
	override fun getFromNode() = try {
	  json.decodeFromString(ser, prefs.get(name, null))
	} catch (e: SerializationException) {
	  warn("got $e when trying to load preference node \"$name\"")
	  e.printReport()
	  defaultValue
	}

	override fun putIntoNode(t: T) =
	  prefs.put(name!!, json.encodeToString(ser, t)).also { if (!silent) println("set pref ${prefs}..${name}=${t}") }
  }

  inner class StringPref(defaultValue: String? = null, name: String? = null):
	  Pref<String>(name = name, defaultValue = defaultValue) {
	override fun getFromNode(): String? = prefs.get(name, defaultValue)
	override fun putIntoNode(t: String) = prefs.put(name!!, t)
  }

  inner class IntPref(defaultValue: Int? = null, name: String? = null):
	  Pref<Int>(name = name, defaultValue = defaultValue) {
	override fun getFromNode() = prefs.getInt(name, defaultValue ?: 0)
	override fun putIntoNode(t: Int) = prefs.putInt(name!!, t)
  }

  inner class BoolPref(defaultValue: Boolean? = null, name: String? = null):
	  Pref<Boolean>(name = name, defaultValue = defaultValue) {
	override fun getFromNode() = prefs.getBoolean(name, defaultValue ?: false)
	override fun putIntoNode(t: Boolean) {
	  prefs.putBoolean(name!!, t)
	}
  }
}

