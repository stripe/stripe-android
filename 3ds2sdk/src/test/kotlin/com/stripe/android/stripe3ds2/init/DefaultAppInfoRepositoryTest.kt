package com.stripe.android.stripe3ds2.init

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAppInfoRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = FakeStore()
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sdkAppId should be a UUID`() = runTest {
        val appInfoRepository = DefaultAppInfoRepository(store, 1, testDispatcher)
        assertThat(UUID.fromString(appInfoRepository.get().sdkAppId))
            .isNotNull()
    }

    @Test
    fun `get() should return same value`() = runTest {
        val appInfoRepository = DefaultAppInfoRepository(store, 1, testDispatcher)
        assertThat(appInfoRepository.get())
            .isEqualTo(appInfoRepository.get())
    }

    @Test
    fun `upgrade should change AppInfo`() = runTest {
        assertThat(
            DefaultAppInfoRepository(context, 1, testDispatcher).get()
        ).isNotEqualTo(
            DefaultAppInfoRepository(context, 2, testDispatcher).get()
        )
    }

    @Test
    fun `get() when shared preferences is `() = runTest {
        val appInfoRepository = DefaultAppInfoRepository(
            object : DefaultAppInfoRepository.Store {
                override suspend fun get(): AppInfo? = null

                override fun save(appInfo: AppInfo) {}
            },
            1,
            testDispatcher
        )
        assertThat(UUID.fromString(appInfoRepository.get().sdkAppId))
            .isNotNull()
    }

    private class FakeStore : DefaultAppInfoRepository.Store {
        private val appInfo = AppInfo(UUID.randomUUID().toString(), 50)

        override suspend fun get(): AppInfo? = appInfo

        override fun save(appInfo: AppInfo) {
        }
    }
}
