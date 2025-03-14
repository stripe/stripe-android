package com.stripe.android.cardscan

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.os.BundleCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.ui.core.cardscan.CardScanActivity
import com.stripe.android.ui.core.cardscan.CardScanActivity.Companion.CARD_SCAN_PARCELABLE_NAME
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardScanActivityTest {

    @Test
    fun testInvalidCardScanArgs() {
        val intent =
            Intent(ApplicationProvider.getApplicationContext(), CardScanActivity::class.java)

        val scenario = ActivityScenario.launchActivityForResult<CardScanActivity>(intent)

        assertThat(scenario.result.resultCode)
            .isEqualTo(RESULT_OK)

        val result = scenario.result.resultData.extras?.let {
            BundleCompat.getParcelable(it, CARD_SCAN_PARCELABLE_NAME, CardScanSheetResult::class.java)
        } as? CardScanSheetResult.Failed
        assertThat(result?.error?.message).isEqualTo("CardScanConfiguration not found")
    }
}
