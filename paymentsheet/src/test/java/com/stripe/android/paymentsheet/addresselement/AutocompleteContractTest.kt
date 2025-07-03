package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AutocompleteContract.EXTRA_ARGS
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutocompleteContractTest {
    @Test
    fun `on create intent, should have expected extras`() {
        val args = AutocompleteContract.Args(
            id = "123",
            googlePlacesApiKey = "gp_123",
            country = "US",
            appearance = PaymentSheet.Appearance.Builder()
                .colorsDark(PaymentSheet.Colors.defaultLight)
                .build()
        )

        val intent = AutocompleteContract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = args,
        )

        val extras = intent.extras

        assertThat(extras?.getParcelable<AutocompleteContract.Args>(EXTRA_ARGS))
            .isEqualTo(args)
    }

    @Test
    fun `should parse out address result`() {
        val expectedResult = AutocompleteContract.Result.Address(
            id = "123",
            addressDetails = AddressDetails(
                name = "John Doe",
                address = PaymentSheet.Address(
                    line1 = "123 Apple Street",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "99999"
                )
            )
        )

        val actualResult = AutocompleteContract.parseResult(
            Activity.RESULT_OK,
            intent = Intent().putExtras(expectedResult.toBundle())
        )

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `should parse out enter manually result`() {
        val expectedResult = AutocompleteContract.Result.EnterManually(
            id = "123",
            addressDetails = AddressDetails(
                name = "John Doe",
                address = PaymentSheet.Address(
                    line1 = "123 Apple Street",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "99999"
                )
            )
        )

        val actualResult = AutocompleteContract.parseResult(
            Activity.RESULT_OK,
            intent = Intent().putExtras(expectedResult.toBundle())
        )

        assertThat(actualResult).isEqualTo(expectedResult)
    }
}
