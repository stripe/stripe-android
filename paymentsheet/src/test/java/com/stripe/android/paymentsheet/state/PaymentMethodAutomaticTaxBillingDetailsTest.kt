package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PaymentMethodAutomaticTaxBillingDetailsTest {
    @Test
    fun `detects sufficient automatic tax billing details`(
        @TestParameter(valuesProvider = AutomaticTaxBillingDetailsCaseProvider::class)
        testCase: AutomaticTaxBillingDetailsCase,
    ) {
        val paymentMethod = PaymentMethodFactory.card(
            last4 = "4242",
            billingDetails = testCase.billingDetails,
        )

        assertThat(paymentMethod.hasSufficientBillingDetailsForAutomaticTax())
            .isEqualTo(testCase.isSufficient)
    }
}

internal object AutomaticTaxBillingDetailsCaseProvider : TestParameterValuesProvider() {
    override fun provideValues(
        context: Context?,
    ): List<AutomaticTaxBillingDetailsCase> = automaticTaxBillingDetailsCases

    private val automaticTaxBillingDetailsCases = listOf(
        AutomaticTaxBillingDetailsCase(
            name = "Missing billing details",
            billingDetails = null,
            isSufficient = false,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Missing address",
            billingDetails = PaymentMethod.BillingDetails(),
            isSufficient = false,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Blank country",
            billingDetails = PaymentMethod.BillingDetails(address = Address(country = " ")),
            isSufficient = false,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Lowercase US",
            billingDetails = billingDetails(
                country = "us",
                line1 = "510 Townsend St",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
            ),
            isSufficient = true,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Country only when no additional fields are required",
            billingDetails = billingDetails(country = "DE"),
            isSufficient = true,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Canada without postal code",
            billingDetails = billingDetails(country = "CA"),
            isSufficient = false,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Canada with postal code",
            billingDetails = billingDetails(country = "CA", postalCode = "M5V 3A8"),
            isSufficient = true,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Great Britain with postal code",
            billingDetails = billingDetails(country = "GB", postalCode = "EC1A 1BB"),
            isSufficient = true,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "India with postal code",
            billingDetails = billingDetails(country = "IN", postalCode = "110001"),
            isSufficient = true,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "Puerto Rico without state",
            billingDetails = billingDetails(
                country = "PR",
                line1 = "151 Calle de Tetuan",
                city = "San Juan",
                postalCode = "00901",
            ),
            isSufficient = true,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "United States with blank city",
            billingDetails = billingDetails(
                country = "US",
                line1 = "510 Townsend St",
                city = " ",
                state = "CA",
                postalCode = "94103",
            ),
            isSufficient = false,
        ),
        AutomaticTaxBillingDetailsCase(
            name = "United States with all required fields",
            billingDetails = billingDetails(
                country = "US",
                line1 = "510 Townsend St",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
            ),
            isSufficient = true,
        ),
    )
}

internal data class AutomaticTaxBillingDetailsCase(
    val name: String,
    val billingDetails: PaymentMethod.BillingDetails?,
    val isSufficient: Boolean,
) {
    override fun toString(): String = name
}

private fun billingDetails(
    country: String,
    line1: String? = null,
    city: String? = null,
    state: String? = null,
    postalCode: String? = null,
): PaymentMethod.BillingDetails {
    return PaymentMethod.BillingDetails(
        address = Address(
            country = country,
            line1 = line1,
            city = city,
            state = state,
            postalCode = postalCode,
        ),
    )
}
