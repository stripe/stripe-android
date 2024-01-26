package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.utils.FakeAndroidKeyStore
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkConfigurationCoordinatorTest {
    private val config = LinkConfiguration(
        stripeIntent = StripeIntentFixtures.PI_SUCCEEDED,
        merchantName = MERCHANT_NAME,
        merchantCountryCode = "US",
        customerInfo = LinkConfiguration.CustomerInfo(
            name = CUSTOMER_NAME,
            email = CUSTOMER_EMAIL,
            phone = CUSTOMER_PHONE,
            billingCountryCode = CUSTOMER_BILLING_COUNTRY_CODE,
            shouldPrefill = true,
        ),
        shippingValues = null,
        signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
        passthroughModeEnabled = false,
    )

    private val linkComponentBuilder: LinkComponent.Builder = mock()

    private var linkConfigurationCoordinator = LinkConfigurationCoordinator(linkComponentBuilder)

    init {
        FakeAndroidKeyStore.setup()

        val configurationCapture = argumentCaptor<LinkConfiguration>()

        whenever(linkComponentBuilder.configuration(configurationCapture.capture()))
            .thenReturn(linkComponentBuilder)
        whenever(linkComponentBuilder.build()).thenAnswer {
            val component = mock<LinkComponent>()
            val linkAccountManager = mock<LinkAccountManager>()
            whenever(component.linkAccountManager).thenReturn(linkAccountManager)
            whenever(linkAccountManager.accountStatus).thenReturn(flowOf(AccountStatus.Verified))
            linkAccountManager.stub {
                onBlocking { linkAccountManager.signInWithUserInput(any()) }.doReturn(
                    Result.failure(IllegalStateException("Test"))
                )
            }

            whenever(component.configuration).thenReturn(configurationCapture.lastValue)

            component
        }
    }

    @Test
    fun `verify component is reused for same configuration`() = runTest {
        linkConfigurationCoordinator.getAccountStatusFlow(config)
        val component = linkConfigurationCoordinator.component
        linkConfigurationCoordinator.signInWithUserInput(config, mock<UserInput.SignIn>())
        assertThat(linkConfigurationCoordinator.component).isEqualTo(component)
    }

    @Test
    fun `verify component is recreated for different configuration`() {
        val component = linkConfigurationCoordinator.component
        linkConfigurationCoordinator.getAccountStatusFlow(config.copy(merchantName = "anotherName"))
        assertThat(linkConfigurationCoordinator.component).isNotEqualTo(component)
    }

    companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "email"
        const val CUSTOMER_PHONE = "phone"
        const val CUSTOMER_NAME = "name"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "country_code"
    }
}
