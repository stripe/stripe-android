package com.stripe.android.testing

import android.os.Bundle
import android.util.Base64
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
class QuarantinedTestMatcherTest {

    @Test
    fun `match is true when description matches a quarantined class and method`() {
        val json = """[{"className":"com.example.Foo","testCaseName":"bar"}]"""
        val matcher = matcherForGzipBase64Payload(json)

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isTrue()
    }

    @Test
    fun `match is false when class does not match`() {
        val json = """[{"className":"com.example.Foo","testCaseName":"bar"}]"""
        val matcher = matcherForGzipBase64Payload(json)

        val description = Description.createTestDescription("com.other.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when method does not match`() {
        val json = """[{"className":"com.example.Foo","testCaseName":"bar"}]"""
        val matcher = matcherForGzipBase64Payload(json)

        val description = Description.createTestDescription("com.example.Foo", "baz")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when bitriseQuarantinedTests is absent`() {
        val matcher = QuarantinedTestMatcher(Bundle())

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when bitriseQuarantinedTests is blank after trim`() {
        val bundle = Bundle().apply { putString("bitriseQuarantinedTests", "   \t  ") }
        val matcher = QuarantinedTestMatcher(bundle)

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when payload is not valid gzip base64`() {
        val bundle = Bundle().apply {
            putString("bitriseQuarantinedTests", "not-valid-data!!!")
        }
        val matcher = QuarantinedTestMatcher(bundle)

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when decoded json is not a valid array`() {
        val matcher = matcherForGzipBase64Payload("""{"className":"com.example.Foo","testCaseName":"bar"}""")

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when decoded json is invalid`() {
        val matcher = matcherForGzipBase64Payload("not json at all")

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match respects multiple quarantined entries`() {
        val json =
            """
            [
              {"className":"com.a.First","testCaseName":"m1"},
              {"className":"com.b.Second","testCaseName":"m2"}
            ]
            """.trimIndent()
        val matcher = matcherForGzipBase64Payload(json)

        assertThat(matcher.match(Description.createTestDescription("com.a.First", "m1"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.b.Second", "m2"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.a.First", "m2"))).isFalse()
    }

    @Test
    fun `match ignores array entries with missing or empty className or testCaseName`() {
        val json =
            """
            [
              {"className":"","testCaseName":"x"},
              {"className":"com.ok.Cls","testCaseName":""},
              {"className":"com.ok.Cls","testCaseName":"good"}
            ]
            """.trimIndent()
        val matcher = matcherForGzipBase64Payload(json)

        assertThat(matcher.match(Description.createTestDescription("com.ok.Cls", "good"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.ok.Cls", "x"))).isFalse()
    }

    private fun matcherForGzipBase64Payload(json: String): QuarantinedTestMatcher {
        val compressed = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip -> gzip.write(json.encodeToByteArray()) }
            baos.toByteArray()
        }
        val encoded = Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP)
        val bundle = Bundle().apply { putString("bitriseQuarantinedTests", encoded) }
        return QuarantinedTestMatcher(bundle)
    }
}
