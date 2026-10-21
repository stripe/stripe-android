package com.stripe.android.paymentsheet.addresselement

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.validateAnalyticsRequest
import com.stripe.paymentelementtestpages.AddressElementPage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
internal class AddressElementAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val page = AddressElementPage(testRules.compose, ApplicationProvider.getApplicationContext())

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

    @Test
    fun completingAddressFromAutocompleteReportsTheSelectedResult() {
        networkRule.validateAnalyticsRequest(
            eventName = "mc_address_show",
            productUsage = productUsage,
        )
        networkRule.validateAnalyticsRequest(
            eventName = "mc_address_completed",
            productUsage = productUsage,
            analyticsPayloadField(
                "address_data_blob[auto_complete_result_selected]",
                "true",
            ),
            analyticsPayloadField(
                "address_data_blob[edit_distance]",
                "0",
            ),
        )
        runAddressElementTest(page) {
            present(
                AddressLauncher.Configuration(
                    address = AddressDetails(
                        name = "Real Name",
                        address = PaymentSheet.Address(country = "US"),
                    ),
                    allowedCountries = setOf("US"),
                    autocompleteCountries = setOf("US"),
                    billingAddress = null,
                )
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_address_autocomplete_start",
                productUsage = productUsage,
            )
            networkRule.validateAnalyticsRequest(
                eventName = "mc_address_autocomplete_suggestions",
                productUsage = productUsage,
            )
            networkRule.enqueue(
                method("POST"),
                path("/v1/elements/address/autocomplete"),
                bodyPart("search_text", AUTOCOMPLETE_QUERY),
            ) { response ->
                response.setBody(AUTOCOMPLETE_RESPONSE)
            }
            page.enterAutocompleteQuery(AUTOCOMPLETE_QUERY)

            networkRule.validateAnalyticsRequest(
                eventName = "mc_address_autocomplete_selected",
                productUsage = productUsage,
            )
            networkRule.enqueue(
                method("POST"),
                path("/v1/elements/address/details"),
                bodyPart("place_id", PICKED_PLACE_ID),
            ) { response ->
                response.setBody(PLACE_DETAILS_RESPONSE)
            }
            page.selectAutocompletePrediction(PICKED_LINE1)
            page.clickSave()

            val result = results.awaitItem()
            assertThat(result).isInstanceOf(AddressLauncherResult.Succeeded::class.java)
            val savedAddress = (result as AddressLauncherResult.Succeeded).address.address
            assertThat(savedAddress?.line1).isEqualTo(PICKED_LINE1)
            assertThat(savedAddress?.city).isEqualTo(PICKED_CITY)
        }
    }

    private companion object {
        val productUsage = setOf("PaymentSheet.AddressController")
        const val MERCHANT_LINE1 = "1234 Main St"
        const val AUTOCOMPLETE_QUERY = "123 Market"
        const val PICKED_PLACE_ID = "picked-place-id"
        const val PICKED_LINE1 = "123 Market Street"
        const val PICKED_SECONDARY_TEXT = "San Francisco, CA"
        const val PICKED_CITY = "San Francisco"
        const val PICKED_STATE = "CA"
        const val PICKED_POSTAL_CODE = "94103"
        val AUTOCOMPLETE_RESPONSE = """
            {
              "source": "google",
              "suggestions": [
                {
                  "address": null,
                  "display_data": {
                    "subtitle": "$PICKED_SECONDARY_TEXT",
                    "title": "$PICKED_LINE1"
                  },
                  "place_id": "$PICKED_PLACE_ID"
                }
              ]
            }
        """.trimIndent()
        val PLACE_DETAILS_RESPONSE = """
            {
              "address": {
                "line1": "$PICKED_LINE1",
                "city": "$PICKED_CITY",
                "state": "$PICKED_STATE",
                "postal_code": "$PICKED_POSTAL_CODE",
                "country": "US"
              }
            }
        """.trimIndent()
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
