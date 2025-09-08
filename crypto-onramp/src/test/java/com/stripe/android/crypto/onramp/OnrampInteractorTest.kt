package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.OnrampUpdatePhoneNumberResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkController.ConfigureResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OnrampInteractorTest {
    private val linkController: LinkController = mock()
    private val cryptoApiRepository: CryptoApiRepository = mock()

    private val interactor: OnrampInteractor = OnrampInteractor(
        application = RuntimeEnvironment.getApplication(),
        linkController = linkController,
        cryptoApiRepository = cryptoApiRepository
    )

    @Test
    fun testConfigureIsSuccessful() = runTest {
        whenever(linkController.configure(any())).thenReturn(ConfigureResult.Success)

        val result = interactor.configure(
            OnrampConfiguration(
                merchantDisplayName = "merchant-display-name",
                publishableKey = "pk_test_12345",
                stripeAccountId = "acct_12345",
                appearance = mock()
            )
        )

        assert(result is OnrampConfigurationResult.Completed)
    }

    @Test
    fun testHasLinkAccountIsSuccessful() = runTest {
        val successResult = mock<LinkController.LookupConsumerResult.Success> {
            on { email } doReturn "test@email.com"
            on { isConsumer } doReturn true
        }

        whenever(linkController.lookupConsumer(any())).thenReturn(successResult)

        val result = interactor.hasLinkAccount("test@email.com")

        assert(result is OnrampHasLinkAccountResult.Completed)

        val completed = result as OnrampHasLinkAccountResult.Completed
        assertThat(completed.hasLinkAccount).isTrue()
        verify(linkController).lookupConsumer("test@email.com")
    }

    @Test
    fun testRegisterLinkUserIsSuccessful() = runTest {
        whenever(linkController.registerConsumer(any(), any(), any(), any())).thenReturn(
            LinkController.RegisterConsumerResult.Success
        )

        val mockLinkAccount = mock<LinkController.LinkAccount> {
            on { consumerSessionClientSecret } doReturn "secret_123"
        }
        val mockState = LinkController.State(
            internalLinkAccount = mockLinkAccount,
            merchantLogoUrl = null,
            selectedPaymentMethodPreview = null,
            createdPaymentMethod = null
        )
        whenever(linkController.state(any())).thenReturn(MutableStateFlow(mockState))

        val mockPermissionsResult = mock<CryptoCustomerResponse> {
            on { id } doReturn "customer_123"
        }
        whenever(cryptoApiRepository.grantPartnerMerchantPermissions(any()))
            .thenReturn(Result.success(mockPermissionsResult))

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
    }

    @Test
    fun testUpdatePhoneNumberIsSuccessful() = runTest {
        whenever(linkController.updatePhoneNumber(any())).thenReturn(LinkController.UpdatePhoneNumberResult.Success)
        val result = interactor.updatePhoneNumber("+1234567890")

        assert(result is OnrampUpdatePhoneNumberResult.Completed)
        verify(linkController).updatePhoneNumber("+1234567890")
    }
}
