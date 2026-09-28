package com.stripe.android.common.nfcscan.security

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(RobolectricTestRunner::class)
internal class OsSettingsDevicePropertiesTest {
    private val deviceProperties = OsSettingsDeviceProperties()

    @After
    fun tearDown() {
        ShadowSystemProperties.reset()
    }

    @Test
    fun `getString returns overridden system property value`() {
        ShadowSystemProperties.override(TEST_KEY, TEST_VALUE)

        assertThat(deviceProperties.getString(TEST_KEY)).isEqualTo(TEST_VALUE)
    }

    @Test
    fun `getString returns null for missing property`() {
        assertThat(deviceProperties.getString(MISSING_KEY)).isNull()
    }

    @Test
    fun `getString returns null for empty property value`() {
        ShadowSystemProperties.override(TEST_KEY, "")

        assertThat(deviceProperties.getString(TEST_KEY)).isNull()
    }

    @Test
    fun `getBoolean returns true when property value is true`() {
        ShadowSystemProperties.override(TEST_KEY, "true")

        assertThat(deviceProperties.getBoolean(TEST_KEY)).isTrue()
    }

    @Test
    fun `getBoolean returns false when property value is false`() {
        ShadowSystemProperties.override(TEST_KEY, "false")

        assertThat(deviceProperties.getBoolean(TEST_KEY)).isFalse()
    }

    @Test
    fun `getBoolean returns null when property is missing`() {
        assertThat(deviceProperties.getBoolean(MISSING_KEY)).isNull()
    }

    private companion object {
        const val TEST_KEY = "stripe.test.os_setting"
        const val TEST_VALUE = "test_value"
        const val MISSING_KEY = "stripe.missing.os_setting"
    }
}
