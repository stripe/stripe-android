package com.stripe.android.paymentsheet.addresselement

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
internal class AddressElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val page = AddressElementPage(testRules.compose)

    @Test
    fun completingMerchantProvidedAddressDoesNotReportAutocompleteSelection() {
        networkRule.validateAnalyticsRequest(
            eventName = "mc_address_show",
            productUsage = productUsage,
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_address_completed",
            productUsage = productUsage,
            analyticsPayloadField(
                "address_data_blob[auto_complete_result_selected]",
                "false",
            ),
            not(hasQueryParam("address_data_blob[edit_distance]")),
        )

        runAddressElementTest(page) {
            present(
                AddressLauncher.Configuration(
                    address = merchantAddress,
                    allowedCountries = setOf("US"),
                    autocompleteCountries = setOf("CA"),
                )
            )
            page.assertCompleteAddress()
            page.clickSave()

            val result = results.awaitItem()
            assertThat(result).isInstanceOf(AddressLauncherResult.Succeeded::class.java)
            assertThat((result as AddressLauncherResult.Succeeded).address.address?.line1)
                .isEqualTo(MERCHANT_LINE1)
        }
    }

    private companion object {
        val productUsage = setOf("PaymentSheet.AddressController")
        const val MERCHANT_LINE1 = "1234 Main St"
        val merchantAddress = AddressDetails(
            name = "Real Name",
            address = PaymentSheet.Address(
                city = "Boston",
                country = "US",
                line1 = MERCHANT_LINE1,
                line2 = "",
                postalCode = "12345",
                state = "MA",
            ),
            phoneNumber = "+1",
            isCheckboxSelected = false,
        )
    }
}
