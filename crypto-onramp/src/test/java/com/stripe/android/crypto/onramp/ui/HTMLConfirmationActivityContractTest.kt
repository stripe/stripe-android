package com.stripe.android.crypto.onramp.ui

import android.app.Activity
import android.content.Intent
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.PartnerDeclarationType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HTMLConfirmationActivityContractTest {
    private val contract = HTMLConfirmationActivityContract()

    @Test
    fun `confirmed terms preserve declaration through parcel transport`() {
        val content = HTMLConfirmationContent.PartnerTerms(
            declarationId = "copt_decl_123",
            declarationType = PartnerDeclarationType.TermsOfService,
        )
        val intent = Intent().putExtra(
            HTMLConfirmationActivity.RESULT_ARG,
            HTMLConfirmationResult.Confirmed(content),
        )
        val parcel = Parcel.obtain()
        try {
            intent.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = Intent.CREATOR.createFromParcel(parcel)

            val result = contract.parseResult(Activity.RESULT_OK, restored) as HTMLConfirmationResult.Confirmed

            assertThat(result.content).isEqualTo(content)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `missing activity result is cancellation without content`() {
        assertThat(contract.parseResult(Activity.RESULT_CANCELED, null))
            .isEqualTo(HTMLConfirmationResult.Cancelled)
    }
}
