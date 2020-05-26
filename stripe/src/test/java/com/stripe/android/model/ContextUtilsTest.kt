package com.stripe.android.model

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.utils.ContextUtils.packageInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
