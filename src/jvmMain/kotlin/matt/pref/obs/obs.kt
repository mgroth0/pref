package matt.pref.obs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import matt.async.thread.namedThread
import matt.obs.MObservable
import matt.obs.prop.BindableProperty
import matt.pref.PrefNode
import matt.pref.PrefNodeBase
import java.util.prefs.Preferences
import kotlin.reflect.KProperty

open class PrefDomain(val key: String) {
    init {
        require(!key.endsWith("."))
    }

    operator fun get(name: String) = PrefDomain("$key.$name")
}

abstract class ObsPrefNode(
    key: String,
    oldNames: List<String> = listOf(),
    oldKeys: List<String> = listOf(),
    json: Json = Json
) : PrefNodeBase() {

    constructor(key: PrefDomain) : this(key.key)

    init {
        namedThread("ObsPrefNode init Thread", isDaemon = true) {
            oldNames.forEach {
                Preferences.userRoot().node(it).apply {
                    removeNode()
                    flush()
                }
            }
        }
    }

    protected inline fun <reified T : Any> obj(
        defaultValue: T? = null,
        silent: Boolean = false
    ) = ObjObsPrefProvider(serializer<T>(), defaultValue, silent = silent)

//    protected fun <T : Any> objNoInline(
//        cls: KClass<T>,
//        defaultValue: T? = null,
//        silent: Boolean = false,
//    ) =
//        ObjObsPrefProvider(cls.serializer(), defaultValue, silent = silent)

    protected inline fun <reified T : MObservable> obsObj(noinline defaultValue: () -> T) =
        ObsObjObsPrefProvider(serializer<T>(), defaultValue)

    override fun string(defaultValue: String?) = StringObsPrefProvider(defaultValue)
    override fun int(defaultValue: Int?) = IntObsPrefProvider(defaultValue)
    override fun bool(defaultValue: Boolean?) = BoolObsPrefProvider(defaultValue)

    private val prefNode by lazy { PrefNode(key, oldKeys, json) }

    private val prefs by lazy { mutableSetOf<ObsPref<*>>() }

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
            thisRef: Any?,
            property: KProperty<*>
        ): BindableProperty<T?> = obsProp
    }

    protected inner class ObsObjObsPrefProvider<T : MObservable>(
        val ser: KSerializer<T>,
        private val defaultValue: () -> T
    ) {
        operator fun provideDelegate(
            thisRef: Any?,
            prop: KProperty<*>
        ) = ObsObjObsPref(ser, defaultValue, prop.name)
    }

    protected inner class ObsObjObsPref<T : MObservable>(
        ser: KSerializer<T>,
        defaultValue: () -> T,
        key: String
    ) {


        private val thePref = prefNode.ObsObjPref(ser, defaultValue, key)
        private val obsObj by lazy {
            thePref.get().apply {
                observe {
                    println("saving ObsObjobsPref")
                    save()
                }
            }
        }


        /*	private val obsObj by lazy {
              println("setting up pref $pref observer with key $key")
              pref.observe {
                println("saving ObsObjobsPref")
                save()
              }
              pref
            }*/

        /*init {

          obsObj
        }*/

        fun save() {
            thePref.putIntoNode(obsObj)
        }

        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>
        ): T {
            println("returning obsObj")
            return obsObj
        }
    }


    protected inner class ObjObsPrefProvider<T : Any>(
        val ser: KSerializer<T>,
        private val defaultValue: T? = null,
        val silent: Boolean
    ) {
        operator fun provideDelegate(
            thisRef: Any?,
            prop: KProperty<*>
        ) = ObjObsPref(ser, defaultValue, prop.name, silent = silent)
    }

    protected inner class ObjObsPref<T : Any>(
        ser: KSerializer<T>,
        defaultValue: T? = null,
        key: String,
        silent: Boolean
    ) :
        ObsPref<T>() {
        override var pref by prefNode.ObjPref(ser, defaultValue, key, silent = silent)
    }

    protected inner class StringObsPrefProvider(private val defaultValue: String? = null) {
        operator fun provideDelegate(
            thisRef: Any?,
            prop: KProperty<*>
        ) = StringObsPref(defaultValue = defaultValue, prop.name)
    }

    protected inner class StringObsPref(
        defaultValue: String? = null,
        key: String
    ) : ObsPref<String>() {
        override var pref by prefNode.StringPref(defaultValue, key)
    }

    protected inner class IntObsPrefProvider(private val defaultValue: Int? = null) {
        operator fun provideDelegate(
            thisRef: Any?,
            prop: KProperty<*>
        ) = IntObsPref(defaultValue, prop.name)
    }

    protected inner class IntObsPref(
        defaultValue: Int? = null,
        key: String
    ) : ObsPref<Int>() {
        override var pref by prefNode.IntPref(defaultValue, key)
    }

    protected inner class BoolObsPrefProvider(private val defaultValue: Boolean? = null) {
        operator fun provideDelegate(
            thisRef: Any?,
            prop: KProperty<*>
        ) = BoolObsPref(defaultValue, prop.name)
    }

    protected inner class BoolObsPref(
        defaultValue: Boolean? = null,
        key: String
    ) : ObsPref<Boolean>() {
        override var pref by prefNode.BoolPref(defaultValue, key)
    }
}