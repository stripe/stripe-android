package com.stripe.android.testing

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuarantinedTestMatcherTest {

    @Test
    fun `match is true when description matches a quarantined class and method`() {
        val matcher = matcherForMatches(
            QuarantinedTestMatch(className = "com.example.Foo", testCaseName = "bar"),
        )

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isTrue()
    }

    @Test
    fun `match is false when class does not match`() {
        val matcher = matcherForMatches(
            QuarantinedTestMatch(className = "com.example.Foo", testCaseName = "bar"),
        )

        val description = Description.createTestDescription("com.other.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match is false when method does not match`() {
        val matcher = matcherForMatches(
            QuarantinedTestMatch(className = "com.example.Foo", testCaseName = "bar"),
        )

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
    fun `match is false when list is empty`() {
        val matcher = matcherForMatches()

        val description = Description.createTestDescription("com.example.Foo", "bar")
        assertThat(matcher.match(description)).isFalse()
    }

    @Test
    fun `match respects multiple quarantined entries`() {
        val matcher = matcherForMatches(
            QuarantinedTestMatch(className = "com.a.First", testCaseName = "m1"),
            QuarantinedTestMatch(className = "com.b.Second", testCaseName = "m2"),
        )

        assertThat(matcher.match(Description.createTestDescription("com.a.First", "m1"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.b.Second", "m2"))).isTrue()
        assertThat(matcher.match(Description.createTestDescription("com.a.First", "m2"))).isFalse()
    }

    private fun matcherForMatches(vararg matches: QuarantinedTestMatch): QuarantinedTestMatcher {
        val bundle = Bundle().apply {
            putParcelableArrayList("bitriseQuarantinedTests", ArrayList(matches.toList()))
        }
        return QuarantinedTestMatcher(bundle)
    }
}
