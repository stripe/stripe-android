package com.stripe.android.core.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.ContextUtils.packageInfo
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ContextUtilsTest {
    @Test
    fun packageInfo_shouldFetchInfoForCurrentPackage() {
        val packageInfo = PackageInfo()
        val packageManager = mock<PackageManager>().also {
            whenever(it.getPackageInfo("package_name", 0)).thenReturn(packageInfo)
        }
        val context = mock<Context>().also {
            whenever(it.packageName).thenReturn("package_name")
            whenever(it.packageManager).thenReturn(packageManager)
        }

        assertThat(context.packageInfo).isSameInstanceAs(packageInfo)
    }

    @Test
    fun packageInfo_whenExceptionThrown_isNull() {
        val packageManager = mock<PackageManager>().also {
            whenever(it.getPackageInfo("package_name", 0)).thenThrow(PackageManager.NameNotFoundException::class.java)
        }
        val context = mock<Context>().also {
            whenever(it.packageName).thenReturn("package_name")
            whenever(it.packageManager).thenReturn(packageManager)
        }
        assertThat(context.packageInfo).isNull()
    }
}
