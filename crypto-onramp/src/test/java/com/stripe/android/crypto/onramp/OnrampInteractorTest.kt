package com.stripe.android.crypto.onramp

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.AppAttestationException
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.MissingCryptoCustomerException
import com.stripe.android.crypto.onramp.model.CreatePaymentTokenResponse
import com.stripe.android.crypto.onramp.model.CrsCarfDeclaration
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import com.stripe.android.crypto.onramp.model.CryptoNetwork
import com.stripe.android.crypto.onramp.model.GetPlatformSettingsResponse
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.KycRetrieveResponse
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
import com.stripe.android.crypto.onramp.model.OnrampAuthorizeResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampCrsCarfDeclarationResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampLogOutResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterWalletAddressResult
import com.stripe.android.crypto.onramp.model.OnrampRetrieveMissingIdentifiersResult
import com.stripe.android.crypto.onramp.model.OnrampSessionClientSecretProvider
import com.stripe.android.crypto.onramp.model.OnrampStartVerificationResult
import com.stripe.android.crypto.onramp.model.OnrampSubmitIdentifiersResult
import com.stripe.android.crypto.onramp.model.OnrampUpdatePhoneNumberResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyIdentityResult
import com.stripe.android.crypto.onramp.model.OnrampVerifyKycInfoResult
import com.stripe.android.crypto.onramp.model.RefreshKycInfo
import com.stripe.android.crypto.onramp.model.StartIdentityVerificationResponse
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifier
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierAlternativeGroup
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierRequirement
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierRequirements
import com.stripe.android.crypto.onramp.model.compliance.ComplianceIdentifierType
import com.stripe.android.crypto.onramp.model.compliance.ComplianceRegulation
import com.stripe.android.crypto.onramp.model.compliance.SubmitIdentifiersResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.crypto.onramp.ui.CrsCarfDeclarationActivityResult
import com.stripe.android.crypto.onramp.ui.CrsCarfDeclarationScreenAction
import com.stripe.android.crypto.onramp.ui.KycRefreshScreenAction
import com.stripe.android.crypto.onramp.ui.VerifyKycActivityResult
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.ConfigureResult
import com.stripe.android.model.DateOfBirth
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OnrampInteractorTest {
    private val linkController: LinkController = mock()
    private val cryptoApiRepository: CryptoApiRepository = mock()
    private val testAnalyticsService = TestOnrampAnalyticsService()
    private val analyticsServiceFactory: OnrampAnalyticsService.Factory = mock {
        on { create(any()) } doReturn testAnalyticsService
    }

    private val interactor: OnrampInteractor = OnrampInteractor(
        application = RuntimeEnvironment.getApplication(),
        linkController = linkController,
        cryptoApiRepository = cryptoApiRepository,
        analyticsServiceFactory = analyticsServiceFactory,
        checkoutHandler = OnrampSessionClientSecretProvider { "test_secret" },
        savedStateHandle = SavedStateHandle()
    )

    @Test
    fun testConfigureIsSuccessful() = runTest {
        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)

        val result = interactor.configure(createConfigurationState())

        assert(result is OnrampConfigurationResult.Completed)
    }

    @Test
    fun testHasLinkAccountIsSuccessful() = runTest {
        val successResult = mock<LinkController.LookupConsumerResult.Success> {
            on { email } doReturn "test@email.com"
            on { isConsumer } doReturn true
        }

        whenever(linkController.lookupConsumer(any())).thenReturn(successResult)

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.hasLinkAccount("test@email.com")

        assert(result is OnrampHasLinkAccountResult.Completed)

        val completed = result as OnrampHasLinkAccountResult.Completed
        assertThat(completed.hasLinkAccount).isTrue()
        verify(linkController).lookupConsumer("test@email.com")

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.LinkAccountLookupCompleted(hasLinkAccount = true)
        )
    }

    @Test
    fun testRegisterLinkUserIsSuccessful() = runTest {
        whenever(linkController.registerConsumer(any(), any(), any(), any())).thenReturn(
            LinkController.RegisterConsumerResult.Success
        )
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val permissionsResult = CryptoCustomerResponse(id = "customer_123")
        whenever(cryptoApiRepository.createCryptoCustomer(any()))
            .thenReturn(Result.success(permissionsResult))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.registerLinkUser(
            LinkUserInfo(
                email = "email",
                phone = "phone",
                country = "US",
                fullName = "Test User"
            )
        )
        assert(result is OnrampRegisterLinkUserResult.Completed)
        verify(linkController).registerConsumer("email", "phone", "US", "Test User")

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkRegistrationCompleted)
    }

    @Test
    fun testUpdatePhoneNumberIsSuccessful() = runTest {
        whenever(linkController.updatePhoneNumber(any())).thenReturn(LinkController.UpdatePhoneNumberResult.Success)

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.updatePhoneNumber("+1234567890")

        assert(result is OnrampUpdatePhoneNumberResult.Completed)
        verify(linkController).updatePhoneNumber("+1234567890")

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkPhoneNumberUpdated)
    }

    @Test
    fun testRegisterWalletAddressIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        whenever(cryptoApiRepository.setWalletAddress(any(), any(), any()))
            .thenReturn(Result.success(Unit))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.registerWalletAddress(
            walletAddress = "0x1234567890abcdef",
            network = CryptoNetwork.Ethereum
        )

        assert(result is OnrampRegisterWalletAddressResult.Completed)

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.WalletRegistered(CryptoNetwork.Ethereum)
        )
    }

    @Test
    fun testRegisterWalletAddressMapsBackendAttestationError() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)
        whenever(cryptoApiRepository.setWalletAddress(any(), any(), any())).thenReturn(
            Result.failure(
                APIException(
                    stripeError = StripeError(
                        code = "link_failed_to_attest_request",
                        message = "App attestation failed",
                        type = "cannot_proceed",
                        extraFields = mapOf(
                            "reason" to "app_not_play_recognized",
                            "user_message" to "This app couldn't be verified. Install it from Google Play and try again."
                        )
                    ),
                    requestId = "req_123",
                    statusCode = 400,
                )
            )
        )

        interactor.onLinkControllerState(mockLinkStateWithAccount())
        interactor.configure(createConfigurationState())

        val result = interactor.registerWalletAddress(
            walletAddress = "0x1234567890abcdef",
            network = CryptoNetwork.Ethereum
        )

        assertThat(result).isInstanceOf(OnrampRegisterWalletAddressResult.Failed::class.java)

        val error = (result as OnrampRegisterWalletAddressResult.Failed).error
        assertThat(error).isInstanceOf(AppAttestationException::class.java)

        val attestationError = error as AppAttestationException
        assertThat(attestationError.userMessageResId)
            .isEqualTo(R.string.stripe_onramp_app_attestation_play_user_message)
        assertThat(attestationError.userMessage)
            .isEqualTo("This app couldn't be verified. Install it from Google Play and try again.")
        assertThat(attestationError.reason).isEqualTo("app_not_play_recognized")
        assertThat(attestationError.mode).isEqualTo("test")
        assertThat(attestationError.message)
            .contains("App attestation failed: this app is not recognized by Google Play.")
        assertThat(attestationError.message)
            .contains("package: ${RuntimeEnvironment.getApplication().packageName}")
        assertThat(attestationError.message).contains("operation: register_wallet_address")
        assertThat(attestationError.message).contains("request_id: req_123")
        assertThat(attestationError.message)
            .contains("Docs: https://stripe.com/docs/crypto/onramp/app-attestation")
    }

    @Test
    fun testHasLinkAccountMapsAttestationInvalidRequestError() = runTest {
        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)
        val failedResult = mock<LinkController.LookupConsumerResult.Failed> {
            on { email } doReturn "test@example.com"
            on { error } doReturn InvalidRequestException(
                stripeError = StripeError(
                    code = "link_failed_to_attest_request",
                    message = "App attestation failed",
                    type = "cannot_proceed",
                    extraFields = mapOf(
                        "reason" to "app_not_play_recognized",
                        "user_message" to "This app couldn't be verified. Install it from Google Play and try again."
                    )
                ),
                requestId = "req_456",
                statusCode = 400,
            )
        }
        whenever(linkController.lookupConsumer(any())).thenReturn(failedResult)

        interactor.configure(createConfigurationState())

        val result = interactor.hasLinkAccount("test@example.com")

        assertThat(result).isInstanceOf(OnrampHasLinkAccountResult.Failed::class.java)

        val error = (result as OnrampHasLinkAccountResult.Failed).error
        assertThat(error).isInstanceOf(AppAttestationException::class.java)

        val attestationError = error as AppAttestationException
        assertThat(attestationError.reason).isEqualTo("app_not_play_recognized")
        assertThat(attestationError.mode).isEqualTo("test")
        assertThat(attestationError.message).contains("operation: has_link_account")
        assertThat(attestationError.message).contains("request_id: req_456")
    }

    @Test
    fun appAttestationExceptionMapsKnownReasonsToLocalizedUserMessages() {
        val cause = IllegalStateException("boom")

        listOf(
            "attestation_not_enabled" to
                R.string.stripe_onramp_app_attestation_not_enabled_user_message,
            "app_not_registered" to
                R.string.stripe_onramp_app_attestation_not_registered_user_message,
            "attestation_data_missing" to
                R.string.stripe_onramp_app_attestation_data_missing_user_message,
            "app_not_play_recognized" to
                R.string.stripe_onramp_app_attestation_play_user_message,
            "android_package_name_mismatch" to
                R.string.stripe_onramp_app_attestation_package_name_mismatch_user_message,
            "android_environment_mismatch" to
                R.string.stripe_onramp_app_attestation_environment_mismatch_user_message,
            "android_verdict_validation_failed" to
                R.string.stripe_onramp_app_attestation_verdict_validation_failed_user_message,
        ).forEach { (reason, expectedResId) ->
            val error = AppAttestationException(
                reason = reason,
                operation = "register_wallet_address",
                appPackageName = "com.example.app",
                mode = "test",
                sdkVersion = "1.0.0",
                apiErrorCode = "link_failed_to_attest_request",
                apiErrorMessage = "App attestation failed",
                apiUserMessage = null,
                docUrl = null,
                cause = cause,
            )

            assertThat(error.userMessageResId).isEqualTo(expectedResId)
        }
    }

    @Test
    fun testAttachKycInfoIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        whenever(cryptoApiRepository.collectKycData(any(), any()))
            .thenReturn(Result.success(Unit))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val kycInfo = KycInfo(
            firstName = "Test",
            lastName = "User",
            idNumber = "999-88-7777",
            dateOfBirth = DateOfBirth(day = 1, month = 3, year = 1975),
            address = PaymentSheet.Address(city = "Orlando", state = "FL")
        )
        val result = interactor.attachKycInfo(kycInfo)
        assertThat(result).isInstanceOf(OnrampAttachKycInfoResult.Completed::class.java)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.KycInfoSubmitted)
    }

    @Test
    fun testRetrieveMissingIdentifiersIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val requirements = ComplianceIdentifierRequirements(
            identifiers = listOf(
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.MT_NIC,
                    regulation = ComplianceRegulation.EuMica
                ),
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.FR_SPI,
                    regulation = ComplianceRegulation.EuCarf
                )
            ),
            alternatives = listOf(
                ComplianceIdentifierAlternativeGroup(
                    originalMissingIdentifiers = listOf(ComplianceIdentifierType.MT_NIC),
                    alternativeMissingIdentifiers = listOf(ComplianceIdentifierType.MT_PP)
                )
            )
        )
        whenever(cryptoApiRepository.retrieveMissingIdentifiers(any()))
            .thenReturn(Result.success(requirements))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.retrieveMissingIdentifiers()

        assertThat(result).isInstanceOf(OnrampRetrieveMissingIdentifiersResult.Completed::class.java)
        val completed = result as OnrampRetrieveMissingIdentifiersResult.Completed
        assertThat(completed.requirements).isEqualTo(requirements)
        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.MissingIdentifiersRetrieved)
    }

    @Test
    fun testSubmitIdentifiersIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val submissionResult = SubmitIdentifiersResult(
            valid = true,
            identifiers = listOf(
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.DE_STN,
                    regulation = ComplianceRegulation.EuCarf
                ),
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.MT_NIC,
                    regulation = ComplianceRegulation.EuCarf
                ),
                ComplianceIdentifierRequirement(
                    type = ComplianceIdentifierType.MT_NIC,
                    regulation = ComplianceRegulation.EuMica
                )
            ),
            alternatives = listOf(
                ComplianceIdentifierAlternativeGroup(
                    originalMissingIdentifiers = listOf(ComplianceIdentifierType.MT_NIC),
                    alternativeMissingIdentifiers = listOf(ComplianceIdentifierType.MT_PP)
                )
            ),
            invalidIdentifiers = listOf(ComplianceIdentifierType.DE_STN, ComplianceIdentifierType.MT_NIC)
        )
        whenever(cryptoApiRepository.submitIdentifiers(any(), any()))
            .thenReturn(Result.success(submissionResult))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.submitIdentifiers(
            listOf(
                ComplianceIdentifier()
                    .type(ComplianceIdentifierType.MT_NIC)
                    .value("mica_123")
            )
        )

        assertThat(result).isInstanceOf(OnrampSubmitIdentifiersResult.Completed::class.java)
        val completed = result as OnrampSubmitIdentifiersResult.Completed
        assertThat(completed.result).isEqualTo(submissionResult)
        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.IdentifiersSubmitted(valid = true))
    }

    @Test
    fun testStartCrsCarfDeclarationIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val declaration = CrsCarfDeclaration(
            text = "I confirm this declaration.",
            version = "2026-04-23"
        )
        whenever(cryptoApiRepository.retrieveCrsCarfDeclaration(any()))
            .thenReturn(Result.success(declaration))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.startCrsCarfDeclaration()

        assertThat(result).isInstanceOf(OnrampStartCrsCarfDeclarationResult.Completed::class.java)
        val completed = result as OnrampStartCrsCarfDeclarationResult.Completed
        assertThat(completed.declaration).isEqualTo(declaration)
        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.CrsCarfDeclarationStarted)
    }

    @Test
    fun testStartIdentityVerificationIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val response = StartIdentityVerificationResponse(
            id = "id_123",
            url = "https://stripe.com",
            ephemeralKey = "ek_test"
        )
        whenever(cryptoApiRepository.startIdentityVerification(any()))
            .thenReturn(Result.success(response))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.startIdentityVerification()
        assert(result is OnrampStartVerificationResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.IdentityVerificationStarted)
    }

    @Test
    fun testCreateCryptoPaymentTokenIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithSelectedPaymentPreview()))
        interactor.onLinkControllerState(mockLinkStateWithSelectedPaymentPreview())

        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)
        interactor.configure(createConfigurationState(cryptoCustomerId = "cpt_123"))

        val mockPlatformSettings = mock<GetPlatformSettingsResponse>()
        doReturn("pk_platform_123").whenever(mockPlatformSettings).publishableKey
        whenever(
            cryptoApiRepository.getPlatformSettings(
                cryptoCustomerId = eq("cpt_123"),
                countryHint = anyOrNull()
            )
        ).thenReturn(Result.success(mockPlatformSettings))

        val mockPaymentMethod = createCardPaymentMethod()

        val mockResult = mock<LinkController.CreatePaymentMethodResult.Success> {
            on { paymentMethod } doReturn mockPaymentMethod
        }
        whenever(linkController.createPaymentMethodForOnramp(any())).thenReturn(mockResult)

        val createPaymentTokenResponse = CreatePaymentTokenResponse(id = "crypto_token_123")
        whenever(
            cryptoApiRepository.createPaymentToken(cryptoCustomerId = any(), paymentMethod = any())
        ).thenReturn(Result.success(createPaymentTokenResponse))

        interactor.handlePresentPaymentMethodsResult(
            LinkController.PresentPaymentMethodsResult.Success,
            RuntimeEnvironment.getApplication()
        )

        val result = interactor.createCryptoPaymentToken()
        assertThat(result).isInstanceOf(OnrampCreateCryptoPaymentTokenResult.Completed::class.java)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.CryptoPaymentTokenCreated(null))
    }

    @Test
    fun testCreateCryptoPaymentTokenForGooglePayIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)
        interactor.configure(createConfigurationState(cryptoCustomerId = "cpt_123"))

        val mockPlatformSettings = mock<GetPlatformSettingsResponse>()
        doReturn("pk_platform_123").whenever(mockPlatformSettings).publishableKey
        whenever(
            cryptoApiRepository.getPlatformSettings(
                cryptoCustomerId = eq("cpt_123"),
                countryHint = anyOrNull()
            )
        ).thenReturn(Result.success(mockPlatformSettings))

        val mockPaymentMethod = createCardPaymentMethod()

        val mockResult = mock<LinkController.CreatePaymentMethodResult.Success> {
            on { paymentMethod } doReturn mockPaymentMethod
        }
        whenever(linkController.createPaymentMethodForOnramp(any())).thenReturn(mockResult)

        val createPaymentTokenResponse = CreatePaymentTokenResponse(id = "crypto_token_123")
        whenever(
            cryptoApiRepository.createPaymentToken(cryptoCustomerId = any(), paymentMethod = any())
        ).thenReturn(Result.success(createPaymentTokenResponse))

        val pm = PaymentMethod(
            id = "pm_123456789",
            created = 1550757934255L,
            liveMode = false,
            type = PaymentMethod.Type.Card,
            customerId = "cus_AQsHpvKfKwJDrF",
            code = "card"
        )

        interactor.handleGooglePayPaymentResult(
            GooglePayPaymentMethodLauncher.Result.Completed(pm)
        )

        val result = interactor.createCryptoPaymentToken()
        assertThat(result).isInstanceOf(OnrampCreateCryptoPaymentTokenResult.Completed::class.java)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.CryptoPaymentTokenCreated(null))
    }

    @Test
    fun testLogOutIsSuccessful() = runTest {
        val mockLogOutSuccess = mock<LinkController.LogOutResult.Success>()
        whenever(linkController.logOut()).thenReturn(mockLogOutSuccess)

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.logOut()
        assert(result is OnrampLogOutResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkLogout)
    }

    @Test
    fun testHandleAuthorizeResultConsented() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))
        val permissionsResult = CryptoCustomerResponse(id = "customer_123")
        whenever(cryptoApiRepository.createCryptoCustomer(any()))
            .thenReturn(Result.success(permissionsResult))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.handleAuthorizeResult(LinkController.AuthorizeResult.Consented)
        assert(result is OnrampAuthorizeResult.Consented)

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.LinkAuthorizationCompleted(consented = true)
        )
    }

    @Test
    fun testHandleIdentityVerificationResultCompleted() {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.handleIdentityVerificationResult(
            VerificationFlowResult.Completed
        )

        assert(result is OnrampVerifyIdentityResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.IdentityVerificationCompleted)
    }

    @Test
    fun testHandlePresentPaymentMethodsResultSuccess() {
        val context = RuntimeEnvironment.getApplication()
        val paymentMethodPreview = mockCardPaymentMethodPreview()

        val mockState = LinkController.State(
            internalLinkAccount = null,
            merchantLogoUrl = null,
            selectedPaymentMethodPreview = paymentMethodPreview,
            createdPaymentMethod = null
        )
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockState))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.handlePresentPaymentMethodsResult(
            LinkController.PresentPaymentMethodsResult.Success,
            context
        )
        assert(result is OnrampCollectPaymentMethodResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.CollectPaymentMethodCompleted(null))
    }

    @Test
    fun testOnAuthorize() {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        interactor.onAuthorize()

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.LinkAuthorizationStarted)
    }

    @Test
    fun testOnHandleNextActionError() = runTest {
        val error = RuntimeException("Payment failed")
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        interactor.onHandleNextActionError(error)

        testAnalyticsService.assertContainsEvent(
            OnrampAnalyticsEvent.ErrorOccurred(
                operation = OnrampAnalyticsEvent.ErrorOccurred.Operation.PerformCheckout,
                error = error
            )
        )
    }

    @Test
    fun testAttachKycInfoFailsMissingSecret() = runTest {
        whenever(
            linkController.state(any())
        ).thenReturn(
            MutableStateFlow(mockLinkStateWithAccount(mockLinkAccountWithoutSecret()))
        )

        val kycInfo = KycInfo(
            firstName = "A",
            lastName = "B",
            idNumber = "111-22-3333",
            dateOfBirth = DateOfBirth(1, 1, 1990),
            address = PaymentSheet.Address(city = "City")
        )

        val result = interactor.attachKycInfo(kycInfo)

        assertThat(result).isInstanceOf(OnrampAttachKycInfoResult.Failed::class.java)
        val failed = result as OnrampAttachKycInfoResult.Failed
        assertThat(failed.error).isInstanceOf(MissingConsumerSecretException::class.java)
    }

    @Test
    fun testRegisterWalletAddressFailsMissingSecret() = runTest {
        whenever(
            linkController.state(any())
        ).thenReturn(
            MutableStateFlow(mockLinkStateWithAccount(mockLinkAccountWithoutSecret()))
        )

        val result = interactor.registerWalletAddress(
            walletAddress = "0xabc",
            network = CryptoNetwork.Bitcoin
        )

        assertThat(result).isInstanceOf(OnrampRegisterWalletAddressResult.Failed::class.java)
        val failed = result as OnrampRegisterWalletAddressResult.Failed
        assertThat(failed.error).isInstanceOf(MissingConsumerSecretException::class.java)
    }

    @Test
    fun testStartKycVerificationIsSuccessful() = runTest {
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockLinkStateWithAccount()))

        val response = KycRetrieveResponse(
            firstName = "Test",
            lastName = "User",
            idNumberLastFour = "7777",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(1, 1, 1990),
            address = PaymentSheet.Address(city = "City")
        )

        whenever(
            cryptoApiRepository.retrieveKycInfo(
                consumerSessionClientSecret = any()
            )
        ).thenReturn(Result.success(response))

        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.startKycVerification(null)
        assert(result is OnrampStartKycVerificationResult.Completed)

        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.KycVerificationStarted)
    }

    @Test
    fun testHandleVerifyKycResultConfirmedSuccess() = runTest {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val refreshInfo = RefreshKycInfo(
            firstName = "Jane",
            lastName = "Doe",
            idNumberLastFour = "9999",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(2, 2, 1980),
            address = PaymentSheet.Address(city = "Tampa")
        )
        whenever(
            cryptoApiRepository.refreshKycData(
                kycInfo = any(),
                consumerSessionClientSecret = any()
            )
        ).thenReturn(Result.success(Unit))

        val result = interactor.handleVerifyKycResult(
            VerifyKycActivityResult(
                KycRefreshScreenAction.Confirm(info = refreshInfo)
            )
        )

        assertThat(result).isInstanceOf(OnrampVerifyKycInfoResult.Confirmed::class.java)
        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.KycVerificationCompleted)
    }

    @Test
    fun testHandleVerifyKycResultConfirmedFailure() = runTest {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val error = RuntimeException("refresh failed")
        whenever(
            cryptoApiRepository.refreshKycData(
                kycInfo = any(),
                consumerSessionClientSecret = any()
            )
        ).thenReturn(Result.failure(error))

        val refreshInfo = RefreshKycInfo(
            firstName = "Jane",
            lastName = "Doe",
            idNumberLastFour = "9999",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(2, 2, 1980),
            address = PaymentSheet.Address(city = "Tampa")
        )

        val result = interactor.handleVerifyKycResult(
            VerifyKycActivityResult(
                KycRefreshScreenAction.Confirm(info = refreshInfo)
            )
        )

        assertThat(result).isInstanceOf(OnrampVerifyKycInfoResult.Failed::class.java)
        val failed = result as OnrampVerifyKycInfoResult.Failed
        assertThat(failed.error.message).contains("refresh failed")
    }

    @Test
    fun testHandleVerifyKycResultConfirmedMissingSecret() = runTest {
        whenever(
            linkController.state(any())
        ).thenReturn(
            MutableStateFlow(mockLinkStateWithAccount(mockLinkAccountWithoutSecret()))
        )

        val refreshInfo = RefreshKycInfo(
            firstName = "Jane",
            lastName = "Doe",
            idNumberLastFour = "9999",
            idType = "SOCIAL_SECURITY_NUMBER",
            dateOfBirth = DateOfBirth(2, 2, 1980),
            address = PaymentSheet.Address(city = "Tampa")
        )

        val result = interactor.handleVerifyKycResult(
            VerifyKycActivityResult(
                KycRefreshScreenAction.Confirm(info = refreshInfo)
            )
        )

        assertThat(result).isInstanceOf(OnrampVerifyKycInfoResult.Failed::class.java)
        val failed = result as OnrampVerifyKycInfoResult.Failed
        assertThat(failed.error).isInstanceOf(MissingConsumerSecretException::class.java)
    }

    @Test
    fun testHandleCrsCarfDeclarationResultConfirmedSuccess() = runTest {
        interactor.onLinkControllerState(mockLinkStateWithAccount())
        whenever(cryptoApiRepository.confirmCrsCarfDeclaration(any()))
            .thenReturn(Result.success(Unit))

        val result = interactor.handleCrsCarfDeclarationResult(
            CrsCarfDeclarationActivityResult(
                CrsCarfDeclarationScreenAction.Confirm
            )
        )

        assertThat(result).isInstanceOf(OnrampCrsCarfDeclarationResult.Confirmed::class.java)
        testAnalyticsService.assertContainsEvent(OnrampAnalyticsEvent.CrsCarfDeclarationCompleted)
    }

    @Test
    fun testHandleCrsCarfDeclarationResultCancelled() = runTest {
        val result = interactor.handleCrsCarfDeclarationResult(
            CrsCarfDeclarationActivityResult(
                CrsCarfDeclarationScreenAction.Cancelled
            )
        )

        assertThat(result).isInstanceOf(OnrampCrsCarfDeclarationResult.Cancelled::class.java)
    }

    @Test
    fun testCreateCryptoPaymentTokenFailsMissingCryptoCustomer() = runTest {
        interactor.onLinkControllerState(mockLinkStateWithAccount())

        val result = interactor.createCryptoPaymentToken()

        assertThat(result).isInstanceOf(OnrampCreateCryptoPaymentTokenResult.Failed::class.java)
        val failed = result as OnrampCreateCryptoPaymentTokenResult.Failed
        assertThat(failed.error).isInstanceOf(MissingCryptoCustomerException::class.java)
    }

    private fun mockLinkStateWithAccount(
        account: LinkController.LinkAccount = mockLinkAccount()
    ): LinkController.State = LinkController.State(
        internalLinkAccount = account,
        merchantLogoUrl = null,
        selectedPaymentMethodPreview = null,
        createdPaymentMethod = null,
        elementsSessionId = "test-elements-session-id"
    )

    private fun mockLinkStateWithSelectedPaymentPreview(
        account: LinkController.LinkAccount = mockLinkAccount()
    ): LinkController.State = LinkController.State(
        internalLinkAccount = account,
        merchantLogoUrl = null,
        selectedPaymentMethodPreview = mockCardPaymentMethodPreview(),
        createdPaymentMethod = null,
        elementsSessionId = "test-elements-session-id"
    )

    private fun mockCardPaymentMethodPreview(): LinkController.PaymentMethodPreview =
        LinkController.PaymentMethodPreview(
            imageLoader = {
                BitmapDrawable(
                    null,
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                )
            },
            label = "Visa",
            sublabel = "•••• 4242",
            type = LinkController.PaymentMethodType.Card
        )

    private fun mockLinkAccount(): LinkController.LinkAccount = LinkController.LinkAccount(
        email = "test@email.com",
        redactedPhoneNumber = "***-***-1234",
        sessionState = LinkController.SessionState.LoggedIn,
        consumerSessionClientSecret = "secret_123"
    )

    private fun mockLinkAccountWithoutSecret(): LinkController.LinkAccount = LinkController.LinkAccount(
        email = "test@email.com",
        redactedPhoneNumber = "***-***-1234",
        sessionState = LinkController.SessionState.LoggedIn,
        consumerSessionClientSecret = null
    )

    private fun createConfigurationState(cryptoCustomerId: String? = null): OnrampConfiguration.State =
        OnrampConfiguration()
            .merchantDisplayName("merchant-display-name")
            .publishableKey("pk_test_12345")
            .appearance(mock())
            .cryptoCustomerId(cryptoCustomerId)
            .build()
}
