package com.stripe.android.paymentsheet.paymentdatacollection.upi

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.payments.PaymentFlowResult
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpiAppChooserContractTest {
    @Test
    fun `contract only launches UPI activity with its arguments`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val intent = UpiAppChooserContract().createIntent(context, UpiFixtures.ARGS)

        assertThat(intent.component?.className).isEqualTo(UpiAppChooserActivity::class.java.name)
        assertThat(UpiAppChooserContract.Args.fromIntent(intent)).isEqualTo(UpiFixtures.ARGS)
    }

    @Test
    fun `arguments survive parceling`() {
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(UpiFixtures.ARGS, 0)
            parcel.setDataPosition(0)
            val restored = parcel.readParcelable<UpiAppChooserContract.Args>(javaClass.classLoader)
            assertThat(restored).isEqualTo(UpiFixtures.ARGS)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `result preserves verification data without using Android result code`() {
        val expected = PaymentFlowResult.Unvalidated(
            clientSecret = UpiFixtures.ARGS.clientSecret,
            flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
            stripeAccountId = "acct_upi",
        )
        val actual = UpiAppChooserContract().parseResult(
            Activity.RESULT_CANCELED,
            Intent().putExtras(expected.toBundle()),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `missing result does not claim success`() {
        assertThat(UpiAppChooserContract().parseResult(Activity.RESULT_OK, null))
            .isEqualTo(PaymentFlowResult.Unvalidated())
    }
}
