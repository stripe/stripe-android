package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiKeyFixtures
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FraudDetectionDataStoreTest {
    @Test
    fun `another store reads persisted data for the same credentials`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val data = FraudDetectionDataFixtures.create()
        DefaultFraudDetectionDataStore(context, dispatcher).save(
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, ApiKeyFixtures.DEFAULT_STRIPE_ACCOUNT, data
        )

        val restored = DefaultFraudDetectionDataStore(context, dispatcher).get(
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, ApiKeyFixtures.DEFAULT_STRIPE_ACCOUNT
        )

        assertThat(restored).isEqualTo(data)
    }

    @Test
    fun `different publishable keys have separate persisted data`() = runTest {
        val store = DefaultFraudDetectionDataStore(
            ApplicationProvider.getApplicationContext(), StandardTestDispatcher(testScheduler)
        )
        store.save(
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            ApiKeyFixtures.DEFAULT_STRIPE_ACCOUNT,
            FraudDetectionDataFixtures.create(),
        )

        assertThat(store.get(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, ApiKeyFixtures.DEFAULT_STRIPE_ACCOUNT)).isNull()
    }

    @Test
    fun `connected accounts have separate persisted data`() = runTest {
        val store = DefaultFraudDetectionDataStore(
            ApplicationProvider.getApplicationContext(), StandardTestDispatcher(testScheduler)
        )
        store.save(
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            ApiKeyFixtures.DEFAULT_STRIPE_ACCOUNT,
            FraudDetectionDataFixtures.create(),
        )

        assertThat(store.get(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, ApiKeyFixtures.OTHER_STRIPE_ACCOUNT)).isNull()
    }

    @Test
    fun `platform credentials do not read connected account data`() = runTest {
        val store = DefaultFraudDetectionDataStore(
            ApplicationProvider.getApplicationContext(), StandardTestDispatcher(testScheduler)
        )
        store.save(
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            ApiKeyFixtures.DEFAULT_STRIPE_ACCOUNT,
            FraudDetectionDataFixtures.create(),
        )

        assertThat(store.get(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, null)).isNull()
    }

    @Test
    fun `unscoped legacy data is not imported into a credential namespace`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("FraudDetectionDataStore", Context.MODE_PRIVATE).edit()
            .putString("key_fraud_detection_data", FraudDetectionDataFixtures.create().toJson().toString())
            .apply()
        val store = DefaultFraudDetectionDataStore(context, StandardTestDispatcher(testScheduler))

        assertThat(store.get(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, ApiKeyFixtures.DEFAULT_STRIPE_ACCOUNT)).isNull()
    }
}
