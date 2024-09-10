package com.stripe.android.paymentsheet.cvcrecollection

import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract.Args
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract.Companion.EXTRA_ARGS
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionResult
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CvcRecollectionContractTest {

    @Mock
    private lateinit var context: Context

    private lateinit var contract: CvcRecollectionContract

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        contract = CvcRecollectionContract()
    }

    @Test
    fun testCreateIntent() {
        val input = Args(
            lastFour = "444",
            cardBrand = CardBrand.Visa,
            appearance = PaymentSheet.Appearance(),
            isTestMode = true
        )

        val intent = contract.createIntent(context, input)

        assertThat(intent).isNotNull()
        assertEquals(input, BundleCompat.getParcelable(intent.extras!!, EXTRA_ARGS, Args::class.java))
    }

    @Test
    fun testParseResultSuccessForConfirmedResult() {
        val resultIntent = Intent().apply {
            putExtra(CvcRecollectionResult.EXTRA_RESULT, CvcRecollectionResult.Confirmed("444"))
        }

        val result = contract.parseResult(0, resultIntent)

        assertThat(result).isEqualTo(CvcRecollectionResult.Confirmed("444"))
    }

    @Test
    fun testParseResultSuccessForCancelledResult() {
        val resultIntent = Intent().apply {
            putExtra(CvcRecollectionResult.EXTRA_RESULT, CvcRecollectionResult.Cancelled)
        }

        val result = contract.parseResult(0, resultIntent)

        assertThat(result).isEqualTo(CvcRecollectionResult.Cancelled)
    }

    @Test
    fun testParseResultFailure() {
        val resultIntent = Intent()

        val result = contract.parseResult(0, resultIntent)

        assertThat(result).isEqualTo(CvcRecollectionResult.Cancelled)
    }
}