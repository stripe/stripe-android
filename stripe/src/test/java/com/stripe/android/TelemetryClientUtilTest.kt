package com.stripe.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import java.util.Objects
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryClientUtilTest {

    @Mock
    private val packageManager: PackageManager? = null

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun initWithAppContext_shouldSucceed() {
        assertFalse(TelemetryClientUtil(ApplicationProvider.getApplicationContext())
            .createTelemetryMap()
            .isEmpty())
    }

    @Test
    fun createTelemetryMap_returnsHasExpectedEntries() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val telemetryMap = TelemetryClientUtil(
            FakeUidSupplier(),
            context.resources.displayMetrics,
            "package_name",
            context.packageManager,
            "-5"
        )
            .createTelemetryMap()
        assertEquals(5, telemetryMap.size)

        val firstMap = telemetryMap["a"] as Map<*, *>
        assertEquals(4, firstMap.size)

        assertEquals("Android 9 REL 28", getSingleValue(firstMap, "d"))
        assertEquals("-5", getSingleValue(firstMap, "g"))
        assertEquals(8, (telemetryMap["b"] as Map<*, *>).size)
    }

    @Test
    @Throws(PackageManager.NameNotFoundException::class)
    fun createTelemetryMap_withVersionName_includesVersionName() {
        val packageInfo = PackageInfo()
        packageInfo.versionName = "version_name"

        `when`(packageManager!!.getPackageInfo("package_name", 0))
            .thenReturn(packageInfo)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val telemetryMap = TelemetryClientUtil(
            FakeUidSupplier(),
            context.resources.displayMetrics,
            "package_name",
            packageManager,
            "-5"
        )
            .createTelemetryMap()

        val secondMap = Objects.requireNonNull<Map<*, *>>(telemetryMap["b"] as Map<*, *>?)
        assertEquals(9, secondMap.size)
        assertEquals("version_name", secondMap["l"])
    }

    private fun getSingleValue(map: Map<*, *>, key: String): Any? {
        return (map[key] as Map<*, *>)["v"]
    }
}
