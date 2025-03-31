package com.stripe.android.cardscan

import android.content.Context
import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.TestFactory
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.ui.core.cardscan.CardScanActivity
import com.stripe.android.ui.core.cardscan.CardScanContract
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardScanContractTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testCreateIntent() {
        val contract = CardScanContract()

        val intent = contract.createIntent(
            context = context,
            input = CardScanContract.Args(TestFactory.cardScanConfiguration)
        )

        val actualConfiguration = intent.extras?.let {
            BundleCompat.getParcelable(
                it,
                CardScanActivity.ARGS,
                CardScanConfiguration::class.java
            )
        }
        assertThat(TestFactory.cardScanConfiguration).isEqualTo(actualConfiguration)
    }
}
