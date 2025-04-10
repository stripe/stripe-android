package com.stripe.android.link.repositories

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.link.FakeConsumersApiService
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.PaymentDetailsFixtures
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.EmailSource
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.VerificationType
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.repository.ConsumersApiService
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@Suppress("LargeClass")
@RunWith(RobolectricTestRunner::class)
class LinkApiRepositoryTest {
    private val stripeRepository = mock<StripeRepository>()
    private val consumersApiService = mock<ConsumersApiService>()
    private val errorReporter = FakeErrorReporter()

    private val paymentIntent = mock<PaymentIntent>().apply {
        whenever(clientSecret).thenReturn("secret")
    }

    private val linkRepository = LinkApiRepository(
        application = ApplicationProvider.getApplicationContext(),
        publishableKeyProvider = { PUBLISHABLE_KEY },
        stripeAccountIdProvider = { STRIPE_ACCOUNT_ID },
        stripeRepository = stripeRepository,
        consumersApiService = consumersApiService,
        workContext = Dispatchers.IO,
        locale = Locale.US,
        errorReporter = errorReporter
    )

    @Before
    fun clearErrorReporter() {
        errorReporter.clear()
    }

    @Test
    fun `lookupConsumer sends correct parameters`() = runTest {
        val consumersApiService = FakeConsumersApiService()
        val linkRepository = linkRepository(consumersApiService)

        val result = linkRepository.lookupConsumer(TestFactory.EMAIL)

        assertThat(result).isEqualTo(Result.success(TestFactory.CONSUMER_SESSION_LOOKUP))
        assertThat(consumersApiService.lookupCalls.size).isEqualTo(1)
        val lookup = consumersApiService.lookupCalls.first()
        assertThat(lookup.email).isEqualTo(TestFactory.EMAIL)
        assertThat(lookup.requestSurface).isEqualTo(CONSUMER_SURFACE)
        assertThat(lookup.requestOptions.apiKey).isEqualTo(PUBLISHABLE_KEY)
        assertThat(lookup.requestOptions.stripeAccount).isEqualTo(STRIPE_ACCOUNT_ID)
    }

    @Test
    fun `lookupConsumer catches exception and returns failure`() = runTest {
        val error = RuntimeException("error")
        val consumersApiService = object : FakeConsumersApiService() {
            override suspend fun lookupConsumerSession(
                email: String,
                requestSurface: String,
                doNotLogConsumerFunnelEvent: Boolean,
                requestOptions: ApiRequest.Options
            ): ConsumerSessionLookup {
                throw error
            }
        }
        val linkRepository = linkRepository(consumersApiService)

        val result = linkRepository.lookupConsumer("email")

        assertThat(result).isEqualTo(Result.failure<ConsumerSessionLookup>(error))
    }

    @Test
    fun `mobileLookupConsumer sends correct parameters`() = runTest {
        val consumersApiService = FakeConsumersApiService()
        val linkRepository = linkRepository(consumersApiService)

        val result = linkRepository.mobileLookupConsumer(
            email = TestFactory.EMAIL,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            sessionId = TestFactory.SESSION_ID,
            emailSource = TestFactory.EMAIL_SOURCE
        )

        assertThat(result).isEqualTo(Result.success(TestFactory.CONSUMER_SESSION_LOOKUP))
        assertThat(consumersApiService.mobileLookupCalls.size).isEqualTo(1)
        val lookup = consumersApiService.mobileLookupCalls.first()
        assertThat(lookup.email).isEqualTo(TestFactory.EMAIL)
        assertThat(lookup.requestSurface).isEqualTo(CONSUMER_SURFACE)
        assertThat(lookup.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(lookup.appId).isEqualTo(TestFactory.APP_ID)
        assertThat(lookup.emailSource).isEqualTo(TestFactory.EMAIL_SOURCE)
        assertThat(lookup.sessionId).isEqualTo(TestFactory.SESSION_ID)
        assertThat(lookup.requestOptions.apiKey).isEqualTo(PUBLISHABLE_KEY)
        assertThat(lookup.requestOptions.stripeAccount).isEqualTo(STRIPE_ACCOUNT_ID)
    }

    @Test
    fun `mobileLookupConsumer catches exception and returns failure`() = runTest {
        val error = RuntimeException("error")
        val consumersApiService = object : FakeConsumersApiService() {
            override suspend fun mobileLookupConsumerSession(
                email: String,
                emailSource: EmailSource,
                requestSurface: String,
                verificationToken: String,
                appId: String,
                requestOptions: ApiRequest.Options,
                sessionId: String
            ): ConsumerSessionLookup {
                throw error
            }
        }
        val linkRepository = linkRepository(consumersApiService)

        val result = linkRepository.mobileLookupConsumer(
            email = TestFactory.EMAIL,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            sessionId = TestFactory.SESSION_ID,
            emailSource = TestFactory.EMAIL_SOURCE
        )

        assertThat(result).isEqualTo(Result.failure<ConsumerSessionLookup>(error))
    }

    @Test
    fun `consumerSignUp returns successful result`() = runTest {
        val consumersApiService = FakeConsumersApiService()
        val linkRepository = linkRepository(consumersApiService)

        val result = linkRepository.consumerSignUp(
            email = TestFactory.EMAIL,
            phone = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = TestFactory.CONSENT_ACTION
        )

        val signUpCall = consumersApiService.signUpCalls.firstOrNull()
        val params = signUpCall?.params

        assertThat(params?.email).isEqualTo(TestFactory.EMAIL)
        assertThat(params?.phoneNumber).isEqualTo(TestFactory.CUSTOMER_PHONE)
        assertThat(params?.country).isEqualTo(TestFactory.COUNTRY)
        assertThat(params?.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(params?.requestSurface).isEqualTo(CONSUMER_SURFACE)
        assertThat(params?.consentAction).isEqualTo(TestFactory.CONSENT_ACTION)

        assertThat(result).isEqualTo(Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP))
    }

    @Test
    fun `consumerSignUp catches exception and returns failure`() = runTest {
        val error = RuntimeException("error")
        val consumersApiService = FakeConsumersApiService()
        val linkRepository = linkRepository(consumersApiService)

        consumersApiService.signUpResult = Result.failure(error)

        val result = linkRepository.consumerSignUp(
            email = TestFactory.EMAIL,
            phone = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = TestFactory.CONSENT_ACTION
        )

        assertThat(result).isEqualTo(Result.failure<ConsumerSessionSignup>(error))
    }

    @Test
    fun `mobileSignUp returns successful result`() = runTest {
        val consumersApiService = FakeConsumersApiService()
        val linkRepository = linkRepository(consumersApiService)

        val result = linkRepository.mobileSignUp(
            email = TestFactory.EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = TestFactory.CONSENT_ACTION,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            incentiveEligibilitySession = TestFactory.INCENTIVE_ELIGIBILITY_SESSION,
            amount = TestFactory.AMOUNT,
            currency = TestFactory.CURRENCY
        )

        val signUpCall = consumersApiService.mobileSignUpCalls.firstOrNull()
        val params = signUpCall?.params

        assertThat(params?.email).isEqualTo(TestFactory.EMAIL)
        assertThat(params?.phoneNumber).isEqualTo(TestFactory.CUSTOMER_PHONE)
        assertThat(params?.country).isEqualTo(TestFactory.COUNTRY)
        assertThat(params?.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(params?.requestSurface).isEqualTo(CONSUMER_SURFACE)
        assertThat(params?.consentAction).isEqualTo(TestFactory.CONSENT_ACTION)
        assertThat(params?.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(params?.appId).isEqualTo(TestFactory.APP_ID)

        assertThat(result).isEqualTo(Result.success(TestFactory.CONSUMER_SESSION_SIGN_UP))
    }

    @Test
    fun `mobileSignUp catches exception and returns failure`() = runTest {
        val error = RuntimeException("error")
        val consumersApiService = FakeConsumersApiService()
        val linkRepository = linkRepository(consumersApiService)

        consumersApiService.mobileSignUpResult = Result.failure(error)

        val result = linkRepository.mobileSignUp(
            email = TestFactory.EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = TestFactory.CONSENT_ACTION,
            verificationToken = TestFactory.VERIFICATION_TOKEN,
            appId = TestFactory.APP_ID,
            incentiveEligibilitySession = TestFactory.INCENTIVE_ELIGIBILITY_SESSION,
            amount = TestFactory.AMOUNT,
            currency = TestFactory.CURRENCY
        )

        assertThat(result).isEqualTo(Result.failure<ConsumerSessionSignup>(error))
    }

    @Test
    fun `createPaymentDetails for card sends correct parameters`() = runTest {
        val secret = "secret"
        val email = "email@stripe.com"
        val consumerKey = "key"

        linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            userEmail = email,
            stripeIntent = paymentIntent,
            consumerSessionClientSecret = secret,
            consumerPublishableKey = consumerKey,
            active = false,
        )

        verify(consumersApiService).createPaymentDetails(
            eq(secret),
            argThat<ConsumerPaymentDetailsCreateParams> {
                toParamMap() == mapOf(
                    "type" to "card",
                    "billing_email_address" to "email@stripe.com",
                    "card" to mapOf(
                        "number" to "5555555555554444",
                        "exp_month" to "12",
                        "exp_year" to "2050"
                    ),
                    "billing_address" to mapOf(
                        "country_code" to "US",
                        "postal_code" to "12345"
                    ),
                    "active" to false,
                )
            },
            requestSurface = eq("android_payment_element"),
            requestOptions = eq(ApiRequest.Options(consumerKey)),
        )
    }

    @Test
    fun `createPaymentDetails for card without consumerPublishableKey sends correct parameters`() =
        runTest {
            val secret = "secret"
            val email = "email@stripe.com"

            linkRepository.createCardPaymentDetails(
                paymentMethodCreateParams = cardPaymentMethodCreateParams,
                userEmail = email,
                stripeIntent = paymentIntent,
                consumerSessionClientSecret = secret,
                consumerPublishableKey = null,
                active = false,
            )

            verify(consumersApiService).createPaymentDetails(
                consumerSessionClientSecret = eq(secret),
                paymentDetailsCreateParams = argThat<ConsumerPaymentDetailsCreateParams> {
                    toParamMap() == mapOf(
                        "type" to "card",
                        "billing_email_address" to "email@stripe.com",
                        "card" to mapOf(
                            "number" to "5555555555554444",
                            "exp_month" to "12",
                            "exp_year" to "2050"
                        ),
                        "billing_address" to mapOf(
                            "country_code" to "US",
                            "postal_code" to "12345"
                        ),
                        "active" to false,
                    )
                },
                requestSurface = eq("android_payment_element"),
                requestOptions = eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID)),
            )
        }

    @Test
    fun `createPaymentDetails for card returns new LinkPaymentDetails when successful`() = runTest {
        val consumerSessionSecret = "consumer_session_secret"
        val email = "email@stripe.com"
        val paymentDetails = PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS
        whenever(
            consumersApiService.createPaymentDetails(
                consumerSessionClientSecret = any(),
                paymentDetailsCreateParams = any(),
                requestSurface = any(),
                requestOptions = any(),
            )
        ).thenReturn(Result.success(paymentDetails))

        val result = linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            userEmail = email,
            stripeIntent = paymentIntent,
            consumerSessionClientSecret = consumerSessionSecret,
            consumerPublishableKey = null,
            active = false,
        )

        assertThat(result.isSuccess).isTrue()

        val newLinkPaymentDetails = result.getOrThrow()

        assertThat(newLinkPaymentDetails.paymentDetails)
            .isEqualTo(paymentDetails.paymentDetails.first())
        assertThat(newLinkPaymentDetails.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.createLink(
                    paymentDetails.paymentDetails.first().id,
                    consumerSessionSecret,
                    mapOf("card" to mapOf("cvc" to "123"))
                )
            )
        assertThat(newLinkPaymentDetails.buildFormValues()).isEqualTo(
            mapOf(
                IdentifierSpec.get("type") to "card",
                IdentifierSpec.CardNumber to "5555555555554444",
                IdentifierSpec.CardCvc to "123",
                IdentifierSpec.CardExpMonth to "12",
                IdentifierSpec.CardExpYear to "2050",
                IdentifierSpec.Country to "US",
                IdentifierSpec.PostalCode to "12345"
            )
        )
    }

    @Test
    fun `createPaymentDetails for card catches exception and returns failure`() = runTest {
        whenever(
            consumersApiService.createPaymentDetails(
                consumerSessionClientSecret = any(),
                paymentDetailsCreateParams = any(),
                requestSurface = any(),
                requestOptions = any(),
            )
        ).thenReturn(Result.failure(RuntimeException("error")))

        val result = linkRepository.createCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            userEmail = "email@stripe.com",
            stripeIntent = paymentIntent,
            consumerSessionClientSecret = "secret",
            consumerPublishableKey = null,
            active = false,
        )
        val loggedErrors = errorReporter.getLoggedErrors()

        assertThat(result.isFailure).isTrue()
        assertThat(loggedErrors.size).isEqualTo(1)
        assertThat(loggedErrors.first()).isEqualTo(ErrorReporter.ExpectedErrorEvent.LINK_CREATE_CARD_FAILURE.eventName)
    }

    @Test
    fun `shareCardPaymentDetails returns LinkPaymentDetails_Saved`() = runTest {
        val consumerSessionSecret = "consumer_session_secret"
        val id = "csmrpd*AYq4D_sXdAAAAOQ0"

        whenever(
            stripeRepository.sharePaymentDetails(
                consumerSessionClientSecret = any(),
                id = any(),
                extraParams = anyOrNull(),
                requestOptions = any(),
            )
        ).thenReturn(Result.success("pm_123"))

        val result = linkRepository.shareCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            consumerSessionClientSecret = consumerSessionSecret,
            id = id,
            last4 = "4242",
            allowRedisplay = null,
        )

        assertThat(result.isSuccess).isTrue()
        val savedLinkPaymentDetails = result.getOrThrow() as LinkPaymentDetails.Saved

        verify(stripeRepository).sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionSecret,
            id = id,
            extraParams = mapOf("payment_method_options" to mapOf("card" to mapOf("cvc" to "123"))),
            requestOptions = ApiRequest.Options(apiKey = PUBLISHABLE_KEY, stripeAccount = STRIPE_ACCOUNT_ID)
        )
        assertThat(savedLinkPaymentDetails.paymentDetails)
            .isEqualTo(ConsumerPaymentDetails.Passthrough(id = "pm_123", last4 = "4242"))
        assertThat(savedLinkPaymentDetails.paymentMethodCreateParams)
            .isEqualTo(
                PaymentMethodCreateParams.createLink(
                    "pm_123",
                    consumerSessionSecret,
                    mapOf("card" to mapOf("cvc" to "123"))
                )
            )
    }

    @Test
    fun `when shareCardPaymentDetails fails, an error is reported`() = runTest {
        val consumerSessionSecret = "consumer_session_secret"

        whenever(
            stripeRepository.sharePaymentDetails(
                consumerSessionClientSecret = any(),
                id = any(),
                extraParams = anyOrNull(),
                requestOptions = any(),
            )
        ).thenReturn(Result.failure(RuntimeException("error")))

        val result = linkRepository.shareCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            consumerSessionClientSecret = consumerSessionSecret,
            id = "csmrpd*AYq4D_sXdAAAAOQ0",
            last4 = "4242",
            allowRedisplay = null,
        )
        val loggedErrors = errorReporter.getLoggedErrors()

        assertThat(result.isFailure).isTrue()
        assertThat(loggedErrors.size).isEqualTo(1)
        assertThat(loggedErrors.first()).isEqualTo(ErrorReporter.ExpectedErrorEvent.LINK_SHARE_CARD_FAILURE.eventName)
    }

    @Test
    fun `when shareCardPaymentDetails with allow_redisplay equals null, should have proper extra params`() =
        allowRedisplayTest(allowRedisplay = null)

    @Test
    fun `when shareCardPaymentDetails with allow_redisplay equals UNSPECIFIED, should have proper extra params`() =
        allowRedisplayTest(allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED)

    @Test
    fun `when shareCardPaymentDetails with allow_redisplay equals LIMITED, should have proper extra params`() =
        allowRedisplayTest(allowRedisplay = PaymentMethod.AllowRedisplay.LIMITED)

    @Test
    fun `when shareCardPaymentDetails with allow_redisplay equals ALWAYS, should have proper extra params`() =
        allowRedisplayTest(allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS)

    @Test
    fun `startVerification sends correct parameters`() = runTest {
        val secret = "secret"
        val consumerKey = "key"
        linkRepository.startVerification(secret, consumerKey)

        verify(consumersApiService).startConsumerVerification(
            consumerSessionClientSecret = secret,
            locale = Locale.US,
            requestSurface = CONSUMER_SURFACE,
            type = VerificationType.SMS,
            customEmailType = null,
            connectionsMerchantName = null,
            requestOptions = ApiRequest.Options(consumerKey),
        )
    }

    @Test
    fun `startVerification without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        linkRepository.startVerification(secret, null)

        verify(consumersApiService).startConsumerVerification(
            consumerSessionClientSecret = secret,
            locale = Locale.US,
            requestSurface = CONSUMER_SURFACE,
            type = VerificationType.SMS,
            customEmailType = null,
            connectionsMerchantName = null,
            requestOptions = ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID),
        )
    }

    @Test
    fun `startVerification returns successful result`() = runTest {
        val consumerSession = mock<ConsumerSession>()
        whenever(
            consumersApiService.startConsumerVerification(
                consumerSessionClientSecret = any(),
                locale = any(),
                requestSurface = any(),
                type = any(),
                customEmailType = anyOrNull(),
                connectionsMerchantName = anyOrNull(),
                requestOptions = any(),
            )
        )
            .thenReturn(consumerSession)

        val result = linkRepository.startVerification("secret", "key")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(consumerSession)
    }

    @Test
    fun `startVerification catches exception and returns failure`() = runTest {
        whenever(
            consumersApiService.startConsumerVerification(
                consumerSessionClientSecret = any(),
                locale = any(),
                requestSurface = any(),
                type = any(),
                customEmailType = anyOrNull(),
                connectionsMerchantName = anyOrNull(),
                requestOptions = any(),
            )
        )
            .thenThrow(RuntimeException("error"))

        val result = linkRepository.startVerification("secret", "key")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `listPaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        val consumerKey = "key"
        linkRepository.listPaymentDetails(setOf("card"), secret, consumerKey)

        verify(stripeRepository).listPaymentDetails(
            eq(secret),
            eq(setOf("card")),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `listPaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        linkRepository.listPaymentDetails(setOf("card"), secret, null)

        verify(stripeRepository).listPaymentDetails(
            eq(secret),
            eq(setOf("card")),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `deletePaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val id = "payment_details_id"
        linkRepository.deletePaymentDetails(id, secret, null)

        verify(stripeRepository).deletePaymentDetails(
            eq(secret),
            eq(id),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `deletePaymentDetails returns successful result`() = runTest {
        whenever(stripeRepository.deletePaymentDetails(any(), any(), any())).thenReturn(Result.success(Unit))

        val result = linkRepository.deletePaymentDetails("id", "secret", "key")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `deletePaymentDetails returns error result when repository fails`() = runTest {
        val error = RuntimeException("error")
        whenever(stripeRepository.deletePaymentDetails(any(), any(), any()))
            .thenReturn(Result.failure(error))

        val result = linkRepository.deletePaymentDetails("id", "secret", "key")

        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }

    @Test
    fun `updatePaymentDetails sends correct parameters`() = runTest {
        val secret = "secret"
        val params = ConsumerPaymentDetailsUpdateParams("id")
        val consumerKey = "key"

        linkRepository.updatePaymentDetails(
            params,
            secret,
            consumerKey
        )

        verify(stripeRepository).updatePaymentDetails(
            eq(secret),
            eq(params),
            eq(ApiRequest.Options(consumerKey))
        )
    }

    @Test
    fun `updatePaymentDetails without consumerPublishableKey sends correct parameters`() = runTest {
        val secret = "secret"
        val params = ConsumerPaymentDetailsUpdateParams("id")

        linkRepository.updatePaymentDetails(
            params,
            secret,
            null
        )

        verify(stripeRepository).updatePaymentDetails(
            eq(secret),
            eq(params),
            eq(ApiRequest.Options(PUBLISHABLE_KEY, STRIPE_ACCOUNT_ID))
        )
    }

    @Test
    fun `updatePaymentDetails returns successful result`() = runTest {
        whenever(stripeRepository.updatePaymentDetails(any(), any(), any()))
            .thenReturn(Result.success(TestFactory.CONSUMER_PAYMENT_DETAILS))

        val result = linkRepository.updatePaymentDetails(
            ConsumerPaymentDetailsUpdateParams("id"),
            "secret",
            "key"
        )

        assertThat(result.getOrNull()).isEqualTo(TestFactory.CONSUMER_PAYMENT_DETAILS)
    }

    @Test
    fun `updatePaymentDetails returns error result when repository fails`() = runTest {
        val error = RuntimeException("error")
        whenever(stripeRepository.updatePaymentDetails(any(), any(), any()))
            .thenReturn(Result.failure(error))

        val result = linkRepository.updatePaymentDetails(
            ConsumerPaymentDetailsUpdateParams("id"),
            "secret",
            "key"
        )

        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }

    private fun allowRedisplayTest(
        allowRedisplay: PaymentMethod.AllowRedisplay?,
    ) = runTest {
        whenever(
            stripeRepository.sharePaymentDetails(
                consumerSessionClientSecret = any(),
                id = any(),
                extraParams = anyOrNull(),
                requestOptions = any(),
            )
        ).thenReturn(Result.success("pm_123"))

        val result = linkRepository.shareCardPaymentDetails(
            paymentMethodCreateParams = cardPaymentMethodCreateParams,
            consumerSessionClientSecret = "consumer_session_secret",
            id = "csmrpd*AYq4D_sXdAAAAOQ0",
            last4 = "4242",
            allowRedisplay = allowRedisplay,
        )

        assertThat(result).isNotNull()

        val extraParamsCaptor = argumentCaptor<Map<String, Any?>>()

        verify(stripeRepository).sharePaymentDetails(
            consumerSessionClientSecret = any(),
            id = any(),
            extraParams = extraParamsCaptor.capture(),
            requestOptions = any(),
        )

        val extraParams = extraParamsCaptor.firstValue

        if (allowRedisplay == null) {
            assertThat(extraParams).doesNotContainKey("allow_redisplay")
        } else {
            assertThat(extraParams).containsEntry("allow_redisplay", allowRedisplay.value)
        }
    }

    private fun linkRepository(
        consumersApiService: ConsumersApiService = FakeConsumersApiService()
    ): LinkApiRepository {
        return LinkApiRepository(
            application = ApplicationProvider.getApplicationContext(),
            publishableKeyProvider = { PUBLISHABLE_KEY },
            stripeAccountIdProvider = { STRIPE_ACCOUNT_ID },
            stripeRepository = stripeRepository,
            consumersApiService = consumersApiService,
            workContext = Dispatchers.IO,
            locale = Locale.US,
            errorReporter = errorReporter
        )
    }

    private val cardPaymentMethodCreateParams =
        FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
            mapOf(
                IdentifierSpec.CardNumber to FormFieldEntry("5555555555554444", true),
                IdentifierSpec.CardCvc to FormFieldEntry("123", true),
                IdentifierSpec.CardExpMonth to FormFieldEntry("12", true),
                IdentifierSpec.CardExpYear to FormFieldEntry("2050", true),
                IdentifierSpec.Country to FormFieldEntry("US", true),
                IdentifierSpec.PostalCode to FormFieldEntry("12345", true)
            ),
            "card",
            false
        )

    companion object {
        const val PUBLISHABLE_KEY = "publishableKey"
        const val STRIPE_ACCOUNT_ID = "stripeAccountId"
        const val CONSUMER_SURFACE = "android_payment_element"
    }
}
