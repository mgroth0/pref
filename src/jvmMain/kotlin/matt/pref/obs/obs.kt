package matt.pref.obs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import matt.obs.MObservable
import matt.obs.prop.BindableProperty
import matt.pref.PrefNode
import matt.pref.PrefNodeBase
import java.util.prefs.Preferences
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

abstract class ObsPrefNode(
  key: String,
  oldNames: List<String>,
  oldKeys: List<String>
): PrefNodeBase() {

  init {
	thread(isDaemon = true) {
	  oldNames.forEach {
		Preferences.userRoot().node(it).apply {
		  removeNode()
		  flush()
		}
	  }
	}
  }

  protected inline fun <reified T: Any> obj(defaultValue: T? = null) =
	ObjObsPrefProvider(T::class.serializer(), defaultValue)

  protected inline fun <reified T: MObservable> obsObj(noinline defaultValue: ()->T) =
	ObsObjObsPrefProvider(T::class.serializer(), defaultValue)

  override fun string(defaultValue: String?) = StringObsPrefProvider(defaultValue)
  override fun int(defaultValue: Int?) = IntObsPrefProvider(defaultValue)
  override fun bool(defaultValue: Boolean?) = BoolObsPrefProvider(defaultValue)

  private val prefNode by lazy { PrefNode(key, oldKeys) }

  private val prefs = mutableSetOf<ObsPref<*>>()

  override fun delete() {
	prefNode.delete()
	prefs.forEach {
	  it.reset()
	}
  }

  protected abstract inner class ObsPref<T> {
	init {
	  prefs += this
	}
	protected abstract var pref: T?
	private val obsProp by lazy {
	  BindableProperty(pref).apply {
		onChange {
		  pref = it
		}
	  }
	}

	fun reset() {
	  obsProp.value = pref
	}

	operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	): BindableProperty<T?> = obsProp
  }

  protected inner class ObsObjObsPrefProvider<T: MObservable>(
	val ser: KSerializer<T>,
	private val defaultValue: ()->T
  ) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = ObsObjObsPref(ser, defaultValue, prop.name)
  }

  protected inner class ObsObjObsPref<T: MObservable>(ser: KSerializer<T>, defaultValue: ()->T, key: String) {
	private val thePref = prefNode.ObsObjPref(ser, defaultValue, key)
	private val pref by thePref
	private val obsObj = pref /*critical*/

	init {
	  println("setting up pref observer")
	  obsObj.observe {
		println("saving ObsObjobsPref")
		thePref.putIntoNode(obsObj)
	  }
	}

	operator fun getValue(
	  thisRef: Any?, property: KProperty<*>
	): T = obsObj
  }


  protected inner class ObjObsPrefProvider<T: Any>(val ser: KSerializer<T>, private val defaultValue: T? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = ObjObsPref(ser, defaultValue, prop.name)
  }

  protected inner class ObjObsPref<T: Any>(ser: KSerializer<T>, defaultValue: T? = null, key: String): ObsPref<T>() {
	override var pref by prefNode.ObjPref(ser, defaultValue, key)
  }

  protected inner class StringObsPrefProvider(private val defaultValue: String? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = StringObsPref(defaultValue = defaultValue, prop.name)
  }

  protected inner class StringObsPref(defaultValue: String? = null, key: String): ObsPref<String>() {
	override var pref by prefNode.StringPref(defaultValue, key)
  }

  protected inner class IntObsPrefProvider(private val defaultValue: Int? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = IntObsPref(defaultValue, prop.name)
  }

  protected inner class IntObsPref(defaultValue: Int? = null, key: String): ObsPref<Int>() {
	override var pref by prefNode.IntPref(defaultValue, key)
  }

  protected inner class BoolObsPrefProvider(private val defaultValue: Boolean? = null) {
	operator fun provideDelegate(
	  thisRef: Any?, prop: KProperty<*>
	) = BoolObsPref(defaultValue, prop.name)
  }

  protected inner class BoolObsPref(defaultValue: Boolean? = null, key: String): ObsPref<Boolean>() {
	override var pref by prefNode.BoolPref(defaultValue, key)
  }
}