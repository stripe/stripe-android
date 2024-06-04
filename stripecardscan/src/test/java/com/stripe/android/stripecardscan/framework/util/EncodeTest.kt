package com.stripe.android.stripecardscan.framework.util

import androidx.test.filters.SmallTest
import com.stripe.android.core.utils.encodeToXWWWFormUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Test
import kotlin.test.assertEquals

class EncodeTest {

    @Test
    @SmallTest
    fun testEncodeToXWWWFormUrl_flatObject() {
        @Serializable
        data class TestClassFlat(
            @SerialName("field_1") val field1: String,
            @SerialName("field_2") val field2: Int,
            @SerialName("field_3") val field3: Boolean
        )

        assertEquals(
            expected = "field_3=true&field_1=aa&field_2=1",
            actual = encodeToXWWWFormUrl(
                serializer = TestClassFlat.serializer(),
                value = TestClassFlat("aa", 1, true)
            )
        )
    }

    @Test
    @SmallTest
    fun testEncodeToXWWWFormUrl_flatObjectWithNulls() {
        @Serializable
        data class TestClassFlat(
            @SerialName("field_1") val field1: String,
            @SerialName("field_2") val field2: Int,
            @SerialName("field_3") val field4: String?,
            @SerialName("field_4") val field3: Boolean
        )

        assertEquals(
            expected = "field_4=true&field_1=aa&field_2=1",
            actual = encodeToXWWWFormUrl(
                serializer = TestClassFlat.serializer(),
                value = TestClassFlat("aa", 1, null, true)
            )
        )
    }

    @Test
    @SmallTest
    fun testEncodeToXWWWFormUrl_nestedObject() {
        @Serializable
        data class TestClassSubObject(
            @SerialName("sub_field_1") val field1: String,
            @SerialName("sub_field_2") val field2: Int
        )

        @Serializable
        data class TestClassBase(
            @SerialName("field_1") val field1: String,
            @SerialName("field_2") val field2: Int,
            @SerialName("field_3") val field3: Boolean,
            @SerialName("field_4") val field4: TestClassSubObject
        )

        assertEquals(
            expected = "field_3=true" +
                "&field_4%5Bsub_field_1%5D=bb" +
                "&field_4%5Bsub_field_2%5D=2" +
                "&field_1=aa" +
                "&field_2=1",
            actual = encodeToXWWWFormUrl(
                serializer = TestClassBase.serializer(),
                value = TestClassBase(
                    field1 = "aa",
                    field2 = 1,
                    field3 = true,
                    field4 = TestClassSubObject(
                        field1 = "bb",
                        field2 = 2
                    )
                )
            )
        )
    }

    @Test
    @SmallTest
    fun testEncodeToXWWWFormUrl_multipleNestedObjects() {
        @Serializable
        data class TestClassSubSubObject(
            @SerialName("sub_sub_field_1") val field1: Int,
            @SerialName("sub_sub_field_2") val field2: Boolean
        )

        @Serializable
        data class TestClassSubObject(
            @SerialName("sub_field_1") val field1: String,
            @SerialName("sub_field_2") val field2: Int,
            @SerialName("sub_field_3") val field3: TestClassSubSubObject
        )

        @Serializable
        data class TestClassBase(
            @SerialName("field_1") val field1: String,
            @SerialName("field_2") val field2: Int,
            @SerialName("field_3") val field3: Boolean,
            @SerialName("field_4") val field4: TestClassSubObject
        )

        assertEquals(
            expected = "field_3=true" +
                "&field_4%5Bsub_field_1%5D=bb" +
                "&field_4%5Bsub_field_3%5D%5Bsub_sub_field_1%5D=3" +
                "&field_4%5Bsub_field_3%5D%5Bsub_sub_field_2%5D=false" +
                "&field_4%5Bsub_field_2%5D=2" +
                "&field_1=aa&field_2=1",
            actual = encodeToXWWWFormUrl(
                serializer = TestClassBase.serializer(),
                value = TestClassBase(
                    field1 = "aa",
                    field2 = 1,
                    field3 = true,
                    field4 = TestClassSubObject(
                        field1 = "bb",
                        field2 = 2,
                        field3 = TestClassSubSubObject(
                            field1 = 3,
                            field2 = false
                        )
                    )
                )
            )
        )
    }

    @Test
    @SmallTest
    fun testEncodeToXWWWFormUrl_multipleNestedObjectsWithList() {
        @Serializable
        data class TestClassSubSubObject(
            @SerialName("sub_sub_field_1") val field1: Int,
            @SerialName("sub_sub_field_2") val field2: Boolean
        )

        @Serializable
        data class TestClassSubObject(
            @SerialName("sub_field_1") val field1: String,
            @SerialName("sub_field_2") val field2: List<String>,
            @SerialName("sub_field_3") val field3: TestClassSubSubObject
        )

        @Serializable
        data class TestClassBase(
            @SerialName("field_1") val field1: String,
            @SerialName("field_2") val field2: Int,
            @SerialName("field_3") val field3: Boolean,
            @SerialName("field_4") val field4: TestClassSubObject
        )

        assertEquals(
            expected = "field_3=true" +
                "&field_4%5Bsub_field_1%5D=bb" +
                "&field_4%5Bsub_field_3%5D%5Bsub_sub_field_1%5D=3" +
                "&field_4%5Bsub_field_3%5D%5Bsub_sub_field_2%5D=false" +
                "&field_4%5Bsub_field_2%5D%5B%5D=2" +
                "&field_4%5Bsub_field_2%5D%5B%5D=4" +
                "&field_4%5Bsub_field_2%5D%5B%5D=6" +
                "&field_1=aa" +
                "&field_2=1",
            actual = encodeToXWWWFormUrl(
                serializer = TestClassBase.serializer(),
                value = TestClassBase(
                    field1 = "aa",
                    field2 = 1,
                    field3 = true,
                    field4 = TestClassSubObject(
                        field1 = "bb",
                        field2 = listOf("2", "4", "6"),
                        field3 = TestClassSubSubObject(
                            field1 = 3,
                            field2 = false
                        )
                    )
                )
            )
        )
    }

    @Test
    @SmallTest
    fun testEncodeToXWWWFormUrl_arrayOfObjects() {
        @Serializable
        data class TestClassSimple(
            @SerialName("sub_field_1") val field1: String,
            @SerialName("sub_field_2") val field2: String
        )

        @Serializable
        data class TestClassArray(
            @SerialName("field_1") val field1: String,
            @SerialName("field_2") val field2: List<TestClassSimple>,
            @SerialName("field_3") val field3: Boolean
        )

        assertEquals(
            expected = "field_3=true" +
                "&field_1=aa" +
                "&field_2%5B0%5D%5Bsub_field_1%5D=bb" +
                "&field_2%5B0%5D%5Bsub_field_2%5D=22" +
                "&field_2%5B1%5D%5Bsub_field_1%5D=cc" +
                "&field_2%5B1%5D%5Bsub_field_2%5D=33",
            actual = encodeToXWWWFormUrl(
                serializer = TestClassArray.serializer(),
                value = TestClassArray(
                    field1 = "aa",
                    field2 = listOf(
                        TestClassSimple(
                            field1 = "bb",
                            field2 = "22"
                        ),
                        TestClassSimple(
                            field1 = "cc",
                            field2 = "33"
                        )
                    ),
                    field3 = true
                )
            )
        )
    }
}
