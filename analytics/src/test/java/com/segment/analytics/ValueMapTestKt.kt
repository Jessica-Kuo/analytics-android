package com.segment.analytics

import com.segment.analytics.TestUtils.PROJECT_SETTINGS_JSON_SAMPLE
import org.assertj.core.api.Assertions
import org.assertj.core.data.MapEntry
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ValueMapTestKt {
    lateinit var valueMap: ValueMap
    lateinit var cartographer: Cartographer

    @Before
    fun setUp() {
        initMocks(this)
        valueMap = ValueMap()
        cartographer = Cartographer.INSTANCE
    }

    @Throws(Exception::class)
    fun disallowsNullMap() {
        try {
            ValueMap(null)
            Assertions.fail("Null Map should throw exception.")
        } catch (ignored: IllegalArgumentException) {
        }
    }

    @Throws(Exception::class)
    @Test
    fun emptyMap() {
        Assertions.assertThat(valueMap).hasSize(0).isEmpty()
    }

    @Test
    fun methodsAreForwardedCorrectly() {
        //todo: don't mock!
        val delegate: MutableMap<String, Any> = Mockito.spy(LinkedHashMap())
        val `object`: Any = "foo"

        valueMap = ValueMap(delegate)

        valueMap.clear()
        Mockito.verify<MutableMap<String, Any>>(delegate).clear()

        valueMap.containsKey(`object`)
        Mockito.verify(delegate).containsKey(`object`)

        valueMap.entries
        Mockito.verify(delegate).entries

        valueMap[`object`]
        Mockito.verify(delegate)[`object`]

        valueMap.isEmpty()
        Mockito.verify(delegate).isEmpty()

        valueMap.keys
        Mockito.verify(delegate).keys

        valueMap["foo"] = `object`
        Mockito.verify(delegate)["foo"] = `object`

        val map: MutableMap<String, Any> = LinkedHashMap()
        valueMap.putAll(map)
        Mockito.verify(delegate).putAll(map)

        valueMap.remove(`object`)
        Mockito.verify(delegate).remove(`object`)

        valueMap.size
        Mockito.verify(delegate).size

        valueMap.values
        Mockito.verify(delegate).values

        valueMap["bar"] = `object`
        Mockito.verify(delegate)["bar"] = `object`
    }

    @Throws(Exception::class)
    @Test
    fun simpleConversation() {
        val stringPi = Math.PI.toString()

        valueMap["double_pi"] = Math.PI
        Assertions.assertThat(valueMap.getString("double_pi")).isEqualTo(stringPi)

        valueMap["string_pi"] = Math.PI
        Assertions.assertThat(valueMap.getDouble("string_pi", 0.0)).isEqualTo(stringPi)
    }

    @Throws(Exception::class)
    @Test
    fun enumDeserialization() {
        valueMap["value1"] = MyEnum.VALUE1
        valueMap["value2"] = MyEnum.VALUE2
        val json: String = cartographer.toJson(valueMap)
        // todo: the ordering may be different on different versions of Java
        Assertions.assertThat(json)
                .isIn(
                        "{\"value2\":\"VALUE2\",\"value1\":\"VALUE1\"}",
                        "{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}")
        valueMap = ValueMap(cartographer.fromJson("{\"value1\":\"VALUE1\",\"value2\":\"VALUE2\"}"))
        Assertions.assertThat(valueMap)
                .contains(MapEntry.entry("value1", "VALUE1"))
                .contains(MapEntry.entry("value2", "VALUE2"))
        Assertions.assertThat(valueMap.getEnum(MyEnum::class.java, "value1")).isEqualTo(MyEnum.VALUE1)
        Assertions.assertThat(valueMap.getEnum(MyEnum::class.java, "value2")).isEqualTo(MyEnum.VALUE2)
    }

    @Test
    fun allowsNullValues() {
        valueMap[null] = "foo"
        valueMap["foo"] = null
    }

    @Throws(Exception::class)
    @Test
    fun nestedMaps() {
        val nested = ValueMap()
        nested["value"] = "box"
        valueMap["nested"] = nested

        Assertions.assertThat(valueMap).hasSize(1).contains(MapEntry.entry("nested", nested))
        Assertions.assertThat(cartographer.toJson(valueMap)).isEqualTo("{\"nested\":{\"value\":\"box\"}}")

        valueMap = ValueMap(cartographer.fromJson("{\"nested\":{\"value\":\"box\"}}"))
        Assertions.assertThat(valueMap).hasSize(1).contains(MapEntry.entry("nested", nested))
    }

    private enum class MyEnum {
        VALUE1,
        VALUE2
    }

    @Throws(Exception::class)
    @Test
    fun customJsonMapDeserialization() {
        val settings = ValueMapTest.Settings(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE))
        Assertions.assertThat(settings)
                .hasSize(4)
                .containsKey("Amplitude")
                .containsKey("Segment")
                .containsKey("Flurry")
                .containsKey("Mixpanel")

        // Map Constructor
        val mixpanelSettings = settings.mixpanelSettings
        Assertions.assertThat(mixpanelSettings) //
                .contains(MapEntry.entry("token", "f7afe0cb436685f61a2b203254779e02"))
                .contains(MapEntry.entry("people", true))
                .contains(MapEntry.entry("trackNamedPages", true))
                .contains(MapEntry.entry("trackCategorizedPages", true))
                .contains(MapEntry.entry("trackAllPages", false))

        try {
            settings.amplitudeSettings
        } catch (exception: java.lang.AssertionError) {
            Assertions.assertThat(exception)
                    .hasMessage(
                            """
                                Could not create instance of com.segment.analytics.ValueMapTest.AmplitudeSettings.
                                java.lang.NoSuchMethodException: com.segment.analytics.ValueMapTest${"$"}AmplitudeSettings.<init>(java.util.Map)
                                """)
        }
    }

    @Throws(Exception::class)
    @Test
    fun projectSettings() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        val valueMap: ValueMap = ValueMap(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE))

        Assertions.assertThat(valueMap.getValueMap("Amplitude"))
                .isNotNull()
                .hasSize(4)
                .contains(MapEntry.entry("apiKey", "ad3c426eb736d7442a65da8174bc1b1b"))
                .contains(MapEntry.entry("trackNamedPages", true))
                .contains(MapEntry.entry("trackCategorizedPages", true))
                .contains(MapEntry.entry("trackAllPages", false))
        Assertions.assertThat(valueMap.getValueMap("Flurry"))
                .isNotNull()
                .hasSize(4)
                .contains(MapEntry.entry("apiKey", "8DY3D6S7CCWH54RBJ9ZM"))
                .contains(MapEntry.entry("captureUncaughtExceptions", false))
                .contains(MapEntry.entry("useHttps", true))
                .contains(MapEntry.entry("sessionContinueSeconds", 10.0));
    }

    @Throws(Exception::class)
    @Test
    fun toJsonObject() {
        val jsonObject: JSONObject =
                ValueMap(cartographer.fromJson(PROJECT_SETTINGS_JSON_SAMPLE)).toJsonObject()
        val amplitude = jsonObject.getJSONObject("Amplitude")
        Assertions.assertThat(amplitude).isNotNull()
        Assertions.assertThat(amplitude.length()).isEqualTo(4)
        Assertions.assertThat(amplitude.getString("apiKey")).isEqualTo("ad3c426eb736d7442a65da8174bc1b1b")
        Assertions.assertThat(amplitude.getBoolean("trackNamedPages")).isTrue()
        Assertions.assertThat(amplitude.getBoolean("trackCategorizedPages")).isTrue()
        Assertions.assertThat(amplitude.getBoolean("trackAllPages")).isFalse()

        val flurry = jsonObject.getJSONObject("Flurry")
        Assertions.assertThat(flurry).isNotNull()
        Assertions.assertThat(flurry.length()).isEqualTo(4)
        Assertions.assertThat(flurry.getString("apiKey")).isEqualTo("8DY3D6S7CCWH54RBJ9ZM")
        Assertions.assertThat(flurry.getBoolean("useHttps")).isTrue()
        Assertions.assertThat(flurry.getBoolean("captureUncaughtExceptions")).isFalse()
        Assertions.assertThat(flurry.getDouble("sessionContinueSeconds")).isEqualTo(10.0)
    }

    @Throws(Exception::class)
    @Test
    fun toJsonObjectWithNullValue() {
        valueMap["foo"] = null

        val jsonObject = valueMap.toJsonObject()
        Assertions.assertThat(jsonObject.get("foo")).isEqualTo(JSONObject.NULL)
    }

    @Test
    fun getInt() {
        Assertions.assertThat(valueMap.getInt("a missing key", 1)).isEqualTo(1)

        valueMap.putValue("a number", 3.14)
        Assertions.assertThat(valueMap.getInt("a number", 0)).isEqualTo(3.14)

        valueMap.putValue("a string number", "892")
        Assertions.assertThat(valueMap.getInt("a string number", 0)).isEqualTo(892)

        valueMap.putValue("a string", "not really an int")
        Assertions.assertThat(valueMap.getInt("a string", 0)).isEqualTo(0)
    }

    @Test
    fun getLong() {
        Assertions.assertThat(valueMap.getLong("a missing key", 2)).isEqualTo(2)

        valueMap.putValue("a number", 3.14)
        Assertions.assertThat(valueMap.getLong("a number", 0)).isEqualTo(3)

        valueMap.putValue("a string number", "88")
        Assertions.assertThat(valueMap.getLong("a string number", 0)).isEqualTo(88)

        valueMap.putValue("a string", "not really a long")
        Assertions.assertThat(valueMap.getInt("a string", 0)).isEqualTo(0)
    }

    @Test
    fun getFloat() {
        Assertions.assertThat(valueMap.getFloat("foo", 0F)).isEqualTo(0)

        valueMap.putValue("foo", 3.14)
        Assertions.assertThat(valueMap.getFloat("foo", 0F)).isEqualTo(3.14F)
    }


    class Settings(fromJson: MutableMap<String, Any>) : ValueMap()
    {
        @Throws(IOException::class)
        fun Settings(map: Map<String?, Any?>?) {
        }

        fun getAmplitudeSettings(): AmplitudeSettings? {
            return getValueMap("Amplitude", AmplitudeSettings::class.java)
        }

        fun getMixpanelSettings(): MixpanelSettings {
            return getValueMap("Mixpanel", MixpanelSettings::class.java)
        }
    }

    class MixpanelSettings(delegate: Map<String, Any>) : ValueMap(delegate)


    class AmplitudeSettings(json: String) : ValueMap() {
        init {
            throw AssertionError("string constuctors must not be called when deserializing")
        }
    }

}