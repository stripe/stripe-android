package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.FakeFraudDetectionDataRepository
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenCreateParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.ShippingInformation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.net.HttpURLConnection
import java.util.Calendar
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeApiRepositoryConfirmationTokenTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val stripeNetworkClient: StripeNetworkClient = mock()
    private val analyticsRequestExecutor: AnalyticsRequestExecutor = mock()
    private val fraudDetectionDataRepository: FraudDetectionDataRepository = mock()

    private val apiRequestArgumentCaptor: KArgumentCaptor<ApiRequest> = argumentCaptor()
    private val analyticsRequestArgumentCaptor: KArgumentCaptor<AnalyticsRequest> = argumentCaptor()

    private lateinit var stripeApiRepository: StripeApiRepository

    @BeforeTest
    fun setup() {
        whenever(fraudDetectionDataRepository.getCached()).thenReturn(
            FraudDetectionData(
                guid = UUID.randomUUID().toString(),
                muid = UUID.randomUUID().toString(),
                sid = UUID.randomUUID().toString(),
                timestamp = Calendar.getInstance().timeInMillis,
            )
        )

        stripeApiRepository = StripeApiRepository(
            context = context,
            publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
            requestSurface = StripeRepository.DEFAULT_REQUEST_SURFACE,
            stripeNetworkClient = stripeNetworkClient,
            analyticsRequestExecutor = analyticsRequestExecutor,
            fraudDetectionDataRepository = fraudDetectionDataRepository,
            workContext = testDispatcher
        )
    }

    @Test
    fun createConfirmationToken_withCardPaymentMethod_shouldReturnExpectedObject() = runTest {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            returnUrl = "https://example.com/return",
            receiptEmail = "test@example.com",
            productUsageTokens = setOf("test_token")
        )

        val expectedConfirmationToken = createExpectedConfirmationToken()
        val successResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = CONFIRMATION_TOKEN_RESPONSE_JSON,
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(successResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedConfirmationToken)

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val capturedRequest = apiRequestArgumentCaptor.firstValue
        assertThat(capturedRequest.url).isEqualTo("https://api.stripe.com/v1/confirmation_tokens")
        assertThat(capturedRequest.method).isEqualTo(StripeRequest.Method.POST)

        // Verify request parameters
        val params = capturedRequest.params!!
        assertThat(params["payment_method_type"]).isEqualTo("card")
        assertThat(params["payment_method_data"]).isEqualTo(paymentMethodCreateParams.toParamMap())
        assertThat(params["return_url"]).isEqualTo("https://example.com/return")
        assertThat(params["receipt_email"]).isEqualTo("test@example.com")
    }

    @Test
    fun createConfirmationToken_withPaymentMethodId_shouldReturnExpectedObject() = runTest {
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodId(
            paymentMethodId = "pm_1234567890",
            paymentMethodType = PaymentMethod.Type.Card,
            returnUrl = "https://example.com/return",
            save = true,
            setupFutureUsage = ConfirmationTokenCreateParams.SetupFutureUsage.OnSession
        )

        val expectedConfirmationToken = createExpectedConfirmationToken()
        val successResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = CONFIRMATION_TOKEN_RESPONSE_JSON,
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(successResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedConfirmationToken)

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val capturedRequest = apiRequestArgumentCaptor.firstValue

        // Verify request parameters
        val params = capturedRequest.params!!
        assertThat(params["payment_method_type"]).isEqualTo("card")
        assertThat(params["payment_method"]).isEqualTo("pm_1234567890")
        assertThat(params["return_url"]).isEqualTo("https://example.com/return")
        assertThat(params["save"]).isEqualTo(true)
        assertThat(params["setup_future_usage"]).isEqualTo("on_session")
    }

    @Test
    fun createConfirmationToken_withShippingAndMandateData_shouldIncludeAllParameters() = runTest {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 6,
            expiryYear = 2028,
            cvc = "456"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        
        val shipping = ShippingInformation(
            name = "John Doe",
            address = Address(
                line1 = "123 Main St",
                city = "New York",
                state = "NY",
                postalCode = "10001",
                country = "US"
            ),
            phone = "+1234567890"
        )
        
        val mandateData = MandateDataParams(
            MandateDataParams.Type.Online(
                ipAddress = "192.168.1.1",
                userAgent = "TestUserAgent/1.0"
            )
        )

        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            shipping = shipping,
            mandateData = mandateData
        )

        val expectedConfirmationToken = createExpectedConfirmationToken()
        val successResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = CONFIRMATION_TOKEN_RESPONSE_JSON,
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(successResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isSuccess).isTrue()

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val capturedRequest = apiRequestArgumentCaptor.firstValue

        // Verify request parameters include shipping and mandate data
        val params = capturedRequest.params!!
        assertThat(params["shipping"]).isEqualTo(shipping.toParamMap())
        assertThat(params["mandate_data"]).isEqualTo(mandateData.toParamMap())
    }

    @Test
    fun createConfirmationToken_withNetworkError_shouldReturnFailure() = runTest {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenAnswer { throw IOException("Network connection failed") }

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(APIConnectionException::class.java)
    }

    @Test
    fun createConfirmationToken_withInvalidRequest_shouldReturnFailure() = runTest {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams
        )

        val errorResponse = StripeResponse(
            code = HttpURLConnection.HTTP_BAD_REQUEST,
            body = """
                {
                    "error": {
                        "type": "invalid_request_error",
                        "message": "Invalid payment method type",
                        "code": "parameter_invalid_empty"
                    }
                }
            """.trimIndent(),
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(errorResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidRequestException::class.java)
    }

    @Test
    fun createConfirmationToken_withInvalidJson_shouldReturnFailure() = runTest {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams
        )

        val invalidJsonResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = "invalid json response",
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(invalidJsonResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun createConfirmationToken_shouldFireAnalyticsEvent() = runTest {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            productUsageTokens = setOf("test_token", "analytics_token")
        )

        val expectedConfirmationToken = createExpectedConfirmationToken()
        val successResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = CONFIRMATION_TOKEN_RESPONSE_JSON,
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(successResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isSuccess).isTrue()

        // Verify analytics event was fired
        verify(analyticsRequestExecutor).executeAsync(analyticsRequestArgumentCaptor.capture())
        val analyticsRequest = analyticsRequestArgumentCaptor.firstValue
        assertThat(analyticsRequest.params["event"]).isEqualTo("stripe_android.confirmation_token_creation")
    }

    @Test
    fun createConfirmationToken_shouldIncludeFraudDetectionData() = runTest {
        val cardParams = PaymentMethodCreateParams.Card(
            number = "4242424242424242",
            expiryMonth = 12,
            expiryYear = 2025,
            cvc = "123"
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(cardParams)
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams
        )

        val expectedConfirmationToken = createExpectedConfirmationToken()
        val successResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = CONFIRMATION_TOKEN_RESPONSE_JSON,
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(successResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isSuccess).isTrue()

        // Verify fraud detection data request was made
        verify(fraudDetectionDataRepository).getCached()
    }

    @Test
    fun createConfirmationToken_withUSBankAccount_shouldCreateExpectedRequest() = runTest {
        val usBankAccountParams = PaymentMethodCreateParams.USBankAccount(
            accountNumber = "000123456789",
            routingNumber = "110000000",
            accountType = PaymentMethod.USBankAccount.USBankAccountType.CHECKING,
            accountHolderType = PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(
            usBankAccount = usBankAccountParams,
            billingDetails = PaymentMethod.BillingDetails(
                name = "Jenny Rosen",
                email = "jenny@example.com"
            )
        )
        
        val confirmationTokenCreateParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            setupFutureUsage = ConfirmationTokenCreateParams.SetupFutureUsage.OffSession
        )

        val expectedConfirmationToken = createExpectedConfirmationToken()
        val successResponse = StripeResponse(
            code = HttpURLConnection.HTTP_OK,
            body = CONFIRMATION_TOKEN_RESPONSE_JSON,
            headers = emptyMap()
        )

        whenever(stripeNetworkClient.executeRequest(any<StripeRequest>()))
            .thenReturn(successResponse)

        val result = stripeApiRepository.createConfirmationToken(
            confirmationTokenCreateParams,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        assertThat(result.isSuccess).isTrue()

        verify(stripeNetworkClient).executeRequest(apiRequestArgumentCaptor.capture())
        val capturedRequest = apiRequestArgumentCaptor.firstValue

        // Verify request parameters for US Bank Account
        val params = capturedRequest.params!!
        assertThat(params["payment_method_type"]).isEqualTo("us_bank_account")
        assertThat(params["setup_future_usage"]).isEqualTo("off_session")
    }

    private fun createExpectedConfirmationToken(): ConfirmationToken {
        return ConfirmationToken(
            id = "ctoken_1HqJTZBKXf6hRPQYeJKTEyul",
            `object` = "confirmation_token",
            created = 1609459200L,
            liveMode = false,
            returnUrl = "https://example.com/return"
        )
    }

    companion object {
        private const val CONFIRMATION_TOKEN_RESPONSE_JSON = """
            {
                "id": "ctoken_1HqJTZBKXf6hRPQYeJKTEyul",
                "object": "confirmation_token",
                "created": 1609459200,
                "livemode": false,
                "return_url": "https://example.com/return"
            }
        """
    }
}