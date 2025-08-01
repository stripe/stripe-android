package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.Address
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.Appearance
import com.stripe.android.paymentsheet.addresselement.AutocompleteContract.EXTRA_ARGS
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutocompleteContractTest {
    @Test
    fun `on create intent with PE context, should have expected extras`() = testCreateIntent(
        appearanceContext = AutocompleteAppearanceContext.PaymentElement(
            Appearance.Builder()
                .colorsDark(Appearance.Colors.defaultLight)
                .build()
        ),
    )

    @Test
    fun `on create intent with Link context, should have expected extras`() = testCreateIntent(
        appearanceContext = AutocompleteAppearanceContext.Link,
    )

    @Test
    fun `should parse out address result`() {
        val expectedResult = AutocompleteContract.Result.Address(
            id = "123",
            addressDetails = AddressDetails(
                name = "John Doe",
                address = Address(
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
                address = Address(
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

    private fun testCreateIntent(
        appearanceContext: AutocompleteAppearanceContext,
    ) {
        val args = AutocompleteContract.Args(
            id = "123",
            googlePlacesApiKey = "gp_123",
            country = "US",
            appearanceContext = appearanceContext,
        )

        val intent = AutocompleteContract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = args,
        )

        val extras = intent.extras

        @Suppress("DEPRECATION")
        assertThat(extras?.getParcelable<AutocompleteContract.Args>(EXTRA_ARGS))
            .isEqualTo(args)
    }
}
