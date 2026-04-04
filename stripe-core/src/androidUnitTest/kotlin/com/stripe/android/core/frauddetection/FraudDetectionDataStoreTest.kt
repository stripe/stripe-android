package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class FraudDetectionDataStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val workContext = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        clearStore()
    }

    @AfterTest
    fun tearDown() {
        clearStore()
    }

    @Test
    fun `save() then get() should return original object`() = runTest {
        val fraudDetectionData = FraudDetectionDataFixtures.create(timestamp = 123L)
        val store = createFraudDetectionDataStore(context, workContext)

        store.save(fraudDetectionData)

        assertThat(store.get()).isEqualTo(fraudDetectionData)
    }

    private fun clearStore() {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {
        private const val PREF_FILE = "FraudDetectionDataStore"
    }
}
