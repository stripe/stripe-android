package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestParamsFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun initWithAppContext_shouldSucceed() {
        assertThat(FingerprintRequestParamsFactory(context).createParams(FINGERPRINT_DATA))
            .isNotEmpty()
    }

    @Test
    fun createParams_returnsHasExpectedEntries() {
        val params = FingerprintRequestParamsFactory(
            context.resources.displayMetrics,
            "package_name",
            null,
            "-5"
        ).createParams(FINGERPRINT_DATA)
        assertThat(params)
            .hasSize(5)

        val firstMap = params["a"] as Map<*, *>
        assertThat(firstMap)
            .hasSize(4)

        assertThat(getSingleValue(firstMap, "d"))
            .isEqualTo("Android 9 REL 28")
        assertThat(getSingleValue(firstMap, "g"))
            .isEqualTo("-5")

        val secondMap = params["b"] as Map<*, *>
        assertThat(secondMap)
            .hasSize(9)
        assertThat(secondMap["d"])
            .isEqualTo(FINGERPRINT_DATA.muid)
        assertThat(secondMap["e"])
            .isEqualTo(FINGERPRINT_DATA.sid)
    }

    @Test
    fun createParams_withVersionName_includesVersionName() {
        val params = FingerprintRequestParamsFactory(
            context.resources.displayMetrics,
            "package_name",
            "version_name",
            "-5"
        )
            .createParams(
                FINGERPRINT_DATA
            )

        val secondMap = params["b"] as Map<*, *>
        assertThat(secondMap)
            .hasSize(10)
        assertThat(secondMap["d"])
            .isEqualTo(FINGERPRINT_DATA.muid)
        assertThat(secondMap["e"])
            .isEqualTo(FINGERPRINT_DATA.sid)
        assertThat(secondMap["l"])
            .isEqualTo("version_name")
    }

    private fun getSingleValue(map: Map<*, *>, key: String): String {
        return (map[key] as Map<*, *>)["v"].toString()
    }

    private companion object {
        private val FINGERPRINT_DATA = FingerprintDataFixtures.create()
    }
}
