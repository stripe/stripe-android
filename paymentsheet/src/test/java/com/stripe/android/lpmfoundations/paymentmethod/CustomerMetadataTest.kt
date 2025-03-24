package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import org.junit.Test

internal class CustomerMetadataTest {

    @Test
    fun `Should create 'Permissions' for customer session properly with permissions disabled`() {
        customerSessionPermissionsTest(
            paymentElementDisabled = true,
        ) { permissions ->
            assertThat(permissions.canRemovePaymentMethods).isFalse()
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
            // Always true for `customer_session`
            assertThat(permissions.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should create 'Permissions' for customer session properly with remove permissions enabled`() {
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethodConfigValue = true,
            canRemoveLastPaymentMethod = true
        ) { permissions ->

            assertThat(permissions.canRemovePaymentMethods).isTrue()
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
            // Always true for `customer_session`
            assertThat(permissions.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value is true for legacy ephemeral keys`() {
        legacyEphemeralKeyTestingScenario(
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
        }
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false for legacy ephemeral keys`() {
        legacyEphemeralKeyTestingScenario(
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value & server value are false`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = false,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false & server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = false,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = true,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server value is false`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = false,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false but server value is true`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = true,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value & server value are true`() =
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethod = true,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
        }

    private fun createEnabledMobilePaymentElement(
        isPaymentMethodRemoveEnabled: Boolean = true,
        canRemoveLastPaymentMethod: Boolean = true,
        isPaymentMethodSetAsDefaultEnabled: Boolean = true,
    ): ElementsSession.Customer.Components.MobilePaymentElement.Enabled {
        return ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
            isPaymentMethodSaveEnabled = true,
            isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
            canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
            allowRedisplayOverride = null,
            isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled
        )
    }

    private fun legacyEphemeralKeyTestingScenario(
        allowsRemovalOfLastSavedPaymentMethod: Boolean,
        block: (CustomerMetadata.Permissions) -> Unit
    ) {
        val permissions = CustomerMetadata.Permissions.createForPaymentSheetLegacyEphemeralKey(
            configuration = createConfiguration(
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
            )
        )

        block(permissions)
    }

    private fun customerSessionPermissionsTest(
        paymentElementDisabled: Boolean = false,
        canRemoveLastPaymentMethodConfigValue: Boolean = true,
        canRemoveLastPaymentMethod: Boolean = true,
        canRemovePaymentMethods: Boolean = true,
        block: (CustomerMetadata.Permissions) -> Unit
    ) {
        val mobilePaymentElementComponent = if (paymentElementDisabled) {
            ElementsSession.Customer.Components.MobilePaymentElement.Disabled
        } else {
            createEnabledMobilePaymentElement(
                isPaymentMethodRemoveEnabled = canRemovePaymentMethods,
                canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
            )
        }

        val configuration = createConfiguration(canRemoveLastPaymentMethodConfigValue)
        val customer = createElementsSessionCustomer(
            mobilePaymentElementComponent = mobilePaymentElementComponent,
        )

        val permissions = CustomerMetadata.Permissions.createForPaymentSheetCustomerSession(
            configuration = configuration,
            customer = customer,
        )

        block(permissions)
    }

    private fun createElementsSessionCustomer(
        customerId: String = "cus_1",
        ephemeralKeySecret: String = "ek_1",
        paymentMethods: List<PaymentMethod> = listOf(),
        mobilePaymentElementComponent: ElementsSession.Customer.Components.MobilePaymentElement,
        defaultPaymentMethodId: String? = null,
    ): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = paymentMethods,
            defaultPaymentMethod = defaultPaymentMethodId,
            session = ElementsSession.Customer.Session(
                id = "cuss_1",
                customerId = customerId,
                apiKey = ephemeralKeySecret,
                apiKeyExpiry = 999999999,
                liveMode = false,
                components = ElementsSession.Customer.Components(
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Disabled,
                    mobilePaymentElement = mobilePaymentElementComponent
                )
            ),
        )
    }

    private fun createConfiguration(
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
    ): CommonConfiguration {
        return PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration().copy(
            allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        )
    }
}
