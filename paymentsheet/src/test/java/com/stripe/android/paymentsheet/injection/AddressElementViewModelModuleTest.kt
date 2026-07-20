package com.stripe.android.paymentsheet.injection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class AddressElementViewModelModuleTest {
    @Test
    fun `provideStripeAutocompleteApiService uses launch-time publishable key`() = runTest {
        val networkClient = RecordingStripeNetworkClient()
        val service = AddressElementViewModelModule().provideStripeAutocompleteApiService(
            stripeNetworkClient = networkClient,
            args = AddressElementActivityContract.Args(
                publishableKey = "pk_address_launcher",
                config = null,
                stripeAccountId = null,
            ),
        )

        service.findAutocompletePredictions(
            query = "123 Main",
            country = "US",
            sessionToken = "session_123",
            locale = "en-US",
            googleApiKey = null,
        )

        val request = networkClient.lastRequest as ApiRequest
        assertThat(request.options.apiKey).isEqualTo("pk_address_launcher")
        assertThat(request.options.stripeAccount).isNull()
    }

    @Test
    fun `provideStripeAutocompleteApiService uses launch-time stripe account id`() = runTest {
        val networkClient = RecordingStripeNetworkClient()
        val service = AddressElementViewModelModule().provideStripeAutocompleteApiService(
            stripeNetworkClient = networkClient,
            args = AddressElementActivityContract.Args(
                publishableKey = "pk_address_launcher",
                config = null,
                stripeAccountId = "acct_123",
            ),
        )

        service.findAutocompletePredictions(
            query = "123 Main",
            country = "US",
            sessionToken = "session_123",
            locale = "en-US",
            googleApiKey = null,
        )

        val request = networkClient.lastRequest as ApiRequest
        assertThat(request.options.stripeAccount).isEqualTo("acct_123")
    }

    private class RecordingStripeNetworkClient : StripeNetworkClient {
        var lastRequest: StripeRequest? = null

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            lastRequest = request
            return StripeResponse(
                code = 200,
                body = """{"suggestions":[]}""",
            )
        }

        override suspend fun executeRequestForFile(
            request: StripeRequest,
            outputFile: File,
        ): StripeResponse<File> {
            lastRequest = request
            return StripeResponse(
                code = 200,
                body = outputFile,
            )
        }
    }
}
