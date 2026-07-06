package com.stripe.android.common.nfcscan.security

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class GlobalSettingsDevicePropertiesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val deviceProperties = GlobalSettingsDeviceProperties(context)

    @Test
    fun `getString returns value from Settings Global`() {
        Settings.Global.putString(context.contentResolver, TEST_KEY, TEST_VALUE)

        assertThat(deviceProperties.getString(TEST_KEY)).isEqualTo(TEST_VALUE)
    }

    @Test
    fun `getString returns null for missing key`() {
        assertThat(deviceProperties.getString(MISSING_KEY)).isNull()
    }

    @Test
    fun `getBoolean returns true when stored value is non-zero`() {
        Settings.Global.putString(context.contentResolver, TEST_KEY, "1")

        assertThat(deviceProperties.getBoolean(TEST_KEY)).isTrue()
    }

    @Test
    fun `getBoolean returns false when stored value is zero`() {
        Settings.Global.putString(context.contentResolver, TEST_KEY, "0")

        assertThat(deviceProperties.getBoolean(TEST_KEY)).isFalse()
    }

    @Test
    fun `getBoolean returns null for missing key`() {
        assertThat(deviceProperties.getBoolean(MISSING_KEY)).isNull()
    }

    private companion object {
        const val TEST_KEY = "stripe_test_global_setting"
        const val TEST_VALUE = "test_value"
        const val MISSING_KEY = "stripe_missing_global_setting"
    }
}
