package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.utils.FakeAndroidKeyStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
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
        ),
        shippingValues = null,
        passthroughModeEnabled = false,
        flags = emptyMap(),
        cardBrandChoice = null,
    )

    private val linkComponentBuilder: LinkComponent.Builder = mock()

    private var linkConfigurationCoordinator = RealLinkConfigurationCoordinator(linkComponentBuilder)

    init {
        FakeAndroidKeyStore.setup()

        val configurationCapture = argumentCaptor<LinkConfiguration>()

        whenever(linkComponentBuilder.configuration(configurationCapture.capture()))
            .thenReturn(linkComponentBuilder)
        whenever(linkComponentBuilder.build()).thenAnswer {
            val component = mock<LinkComponent>()
            val linkAccountManager = FakeLinkAccountManager()
            whenever(component.linkAccountManager).thenReturn(linkAccountManager)
            linkAccountManager.setAccountStatus(AccountStatus.Verified)
            linkAccountManager.signInWithUserInputResult = Result.failure(IllegalStateException("Test"))

            whenever(component.configuration).thenReturn(configurationCapture.lastValue)

            component
        }
    }

    @Test
    fun `verify component is reused for same configuration`() = runTest {
        val component = linkConfigurationCoordinator.getComponent(config)

        linkConfigurationCoordinator.getAccountStatusFlow(config)
        linkConfigurationCoordinator.signInWithUserInput(config, mock<UserInput.SignIn>())

        assertThat(linkConfigurationCoordinator.getComponent(config)).isEqualTo(component)
    }

    @Test
    fun `verify component is recreated for different configuration`() {
        val component = linkConfigurationCoordinator.getComponent(config)

        assertThat(linkConfigurationCoordinator.getComponent(config.copy(merchantName = "anotherName")))
            .isNotEqualTo(component)
    }

    companion object {
        const val MERCHANT_NAME = "merchantName"
        const val CUSTOMER_EMAIL = "email"
        const val CUSTOMER_PHONE = "phone"
        const val CUSTOMER_NAME = "name"
        const val CUSTOMER_BILLING_COUNTRY_CODE = "country_code"
    }
}
