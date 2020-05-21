package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FingerprintRequestParamsFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clientFingerprintDataStore = FakeClientFingerprintDataStore(
        muid = MUID,
        sid = SID
    )

    @Test
    fun initWithAppContext_shouldSucceed() {
        assertThat(FingerprintRequestParamsFactory(context).createParams())
            .isNotEmpty()
    }

    @Test
    fun createParams_returnsHasExpectedEntries() {
        val params = FingerprintRequestParamsFactory(
            context.resources.displayMetrics,
            "package_name",
            null,
            "-5",
            clientFingerprintDataStore
        ).createParams()
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
            .isEqualTo(MUID.toString())
        assertThat(secondMap["e"])
            .isEqualTo(SID.toString())
    }

    @Test
    fun createParams_withVersionName_includesVersionName() {
        val params = FingerprintRequestParamsFactory(
            context.resources.displayMetrics,
            "package_name",
            "version_name",
            "-5",
            clientFingerprintDataStore
        )
            .createParams()

        val secondMap = params["b"] as Map<*, *>
        assertThat(secondMap)
            .hasSize(10)
        assertThat(secondMap["l"])
            .isEqualTo("version_name")
    }

    private fun getSingleValue(map: Map<*, *>, key: String): Any? {
        return (map[key] as Map<*, *>)["v"]
    }

    private companion object {
        private val MUID = UUID.randomUUID()
        private val SID = UUID.randomUUID()
    }
}
