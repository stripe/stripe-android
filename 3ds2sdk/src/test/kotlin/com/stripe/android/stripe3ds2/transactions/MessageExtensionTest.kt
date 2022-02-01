package com.stripe.android.stripe3ds2.transactions

import com.stripe.android.stripe3ds2.utils.ParcelUtils
import org.json.JSONArray
import org.json.JSONException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MessageExtensionTest {

    @Test
    @Throws(JSONException::class)
    fun toJsonObject_shouldReturnExpectedJsonObject() {
        val messageExtensionJson = FIXTURE.toJson()

        assertEquals(
            "name",
            messageExtensionJson.getString(MessageExtension.FIELD_NAME)
        )
        assertEquals(
            "id",
            messageExtensionJson.getString(MessageExtension.FIELD_ID)
        )
        assertTrue(messageExtensionJson.getBoolean(MessageExtension.FIELD_CRITICALITY_INDICATOR))

        val dataJson = messageExtensionJson.getJSONObject(MessageExtension.FIELD_DATA)
        assertEquals("value", dataJson.getString("key"))
    }

    @Test
    @Throws(JSONException::class)
    fun fromJson_withInvalidDataFieldValue_shouldThrowAnException() {
        val exception = assertFailsWith<ChallengeResponseParseException> {
            MessageExtension.fromJson(INVALID_DATA_VALUE_LENGTH_JSON)
        }
        assertEquals(203, exception.code.toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun fromJson_withInvalidNameField_shouldThrowAnException() {
        val exception = assertFailsWith<ChallengeResponseParseException> {
            MessageExtension.fromJson(INVALID_NAME_LENGTH_JSON)
        }
        assertEquals(203, exception.code.toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun fromJson_withInvalidIdField_shouldThrowAnException() {
        val exception = assertFailsWith<ChallengeResponseParseException> {
            MessageExtension.fromJson(INVALID_ID_LENGTH_JSON)
        }
        assertEquals(203, exception.code.toLong())
    }

    @Test
    @Throws(JSONException::class)
    fun fromJson_withInvalidTotalItemCount_shouldThrowAnException() {
        val exception = assertFailsWith<ChallengeResponseParseException> {
            MessageExtension.fromJson(INVALID_TOTAL_ITEM_COUNT_JSON)
        }
        assertEquals(203, exception.code.toLong())
    }

    @Test
    @Throws(JSONException::class, ChallengeResponseParseException::class)
    fun testEquals() {
        assertEquals(MessageExtension.fromJson(JSON), MessageExtension.fromJson(JSON))
    }

    @Test
    fun testParcelable() {
        assertEquals(FIXTURE, ParcelUtils.get(FIXTURE))
    }

    internal companion object {
        internal val FIXTURE: MessageExtension = MessageExtension(
            id = "id",
            name = "name",
            criticalityIndicator = true,
            data = mapOf("key" to "value")
        )

        private val JSON = JSONArray(
            """
            [
               {
                  "name":"extension1",
                  "id":"ID1",
                  "criticalityIndicator":true,
                  "data":{
                     "valueOne":"value"
                  }
               },
               {
                  "name":"extension2",
                  "id":"ID2",
                  "criticalityIndicator":true,
                  "data":{
                     "valueOne":"value1",
                     "valueTwo":"value2"
                  }
               },
               {
                  "name":"sharedData",
                  "id":"ID3",
                  "criticalityIndicator":false,
                  "data":{
                     "value3":"IkpTT05EYXRhIjogew0KImRhdGExIjogInNkYXRhIg0KfQ=="
                  }
               }
            ]
            """.trimIndent()
        )

        private val INVALID_DATA_VALUE_LENGTH_JSON = JSONArray(
            """
            [{
                "name": "testUnrecognisedNonCriticalExtensionField",
                "id": "ID4",
                "criticalityIndicator": false,
                "data": {
                    "text": "Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa. Hercle, burgus pius!, clemens bubo! Eheu. Peritus fraticinida vix carpseriss zirbus est. Germanus, lotus rationes tandem perdere de salvus, regius capio. Heu, fortis turpis! Nunquam experientia tabes. Buxums sunt amors de audax nomen. Lanistas assimilant in teres burdigala! Regius racana acceleratrix experientias calcaria est. Eheu, accentor! Talis heuretes satis talems solem est. Menss manducare in grandis asopus! Heu, acipenser! Est bassus torus, cesaris. Est camerarius valebat, cesaris. Vae. Cedrium de audax xiphias, imperium abnoba! Pol. Zetas sunt musas de salvus mensa..."
                }
            }]
            """.trimIndent()
        )

        private val INVALID_NAME_LENGTH_JSON = JSONArray(
            """
            [{
                "name": "testUnrecognisedNonCriticalExtensionField123456789012345678901234",
                "id": "ID4",
                "criticalityIndicator": false,
                "data": {
                    "text": "This is a non-critical message extension for testing purposes"
                }
            }]
            """.trimIndent()
        )

        private val INVALID_ID_LENGTH_JSON = JSONArray(
            """
            [{
                "name": "testUnrecognisedNonCriticalExtensionField",
                "id": "ID412345678901234567890123456789012345678901234567890123456789012",
                "criticalityIndicator": false,
                "data": {
                    "text": "This is a non-critical message extension for testing purposes"
                }
            }]
            """.trimIndent()
        )

        private val INVALID_TOTAL_ITEM_COUNT_JSON = JSONArray(
            """
            [{
                "name": "testUnrecognisedNonCriticalExtensionField1",
                "id": "ext1",
                "criticalityIndicator": false,
                "data": {
                    "text": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField2",
                "id": "ext2",
                "criticalityIndicator": false,
                "data": {
                    "text2": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField3",
                "id": "ext3",
                "criticalityIndicator": false,
                "data": {
                    "text3": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField4",
                "id": "ext4",
                "criticalityIndicator": false,
                "data": {
                    "text4": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField5",
                "id": "ext5",
                "criticalityIndicator": false,
                "data": {
                    "text5": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField6",
                "id": "ext6",
                "criticalityIndicator": false,
                "data": {
                    "text6": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField7",
                "id": "ext7",
                "criticalityIndicator": false,
                "data": {
                    "text7": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField8",
                "id": "ext8",
                "criticalityIndicator": false,
                "data": {
                    "text8": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField9",
                "id": "ext9",
                "criticalityIndicator": false,
                "data": {
                    "text9": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField10",
                "id": "ext10",
                "criticalityIndicator": false,
                "data": {
                    "text10": "This is a non-critical message extension for testing purposes"
                }
            }, {
                "name": "testUnrecognisedNonCriticalExtensionField11",
                "id": "ext11",
                "criticalityIndicator": false,
                "data": {
                    "text11": "This is a non-critical message extension for testing purposes"
                }
            }]
            """.trimIndent()
        )
    }
}
