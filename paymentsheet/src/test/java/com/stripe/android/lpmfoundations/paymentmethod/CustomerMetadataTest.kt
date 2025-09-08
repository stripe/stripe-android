package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetFixtures
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.SavedSelection
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
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
        ) { permissions ->
            assertThat(permissions.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Full)
            assertThat(permissions.canRemovePaymentMethods).isTrue()
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
            // Always true for `customer_session`
            assertThat(permissions.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should create 'Permissions' for customer session properly with partial remove permissions`() {
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethodConfigValue = true,
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Partial,
        ) { permissions ->
            assertThat(permissions.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Partial)
            assertThat(permissions.canRemovePaymentMethods).isTrue()
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
            // Always true for `customer_session`
            assertThat(permissions.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should create 'Permissions' for customer session properly with disabled remove permissions`() {
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethodConfigValue = true,
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled
        ) { permissions ->
            assertThat(permissions.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.None)
            assertThat(permissions.canRemovePaymentMethods).isFalse()
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
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Disabled,
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
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Disabled,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false & server value is not provided`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false but server value is true`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value is true & server value is not provided`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value & server value are true`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { permissions ->
            assertThat(permissions.canRemoveLastPaymentMethod).isTrue()
        }

    @Test
    fun `'createForCustomerSheet' should have payment method remove permissions of 'Full'`() {
        val customerSheetSession = createCustomerSheetSession(PaymentMethodRemovePermission.Full)

        val permissions = CustomerMetadata.Permissions.createForCustomerSheet(
            configuration = createCustomerSheetConfiguration(),
            customerSheetSession = customerSheetSession
        )

        assertThat(permissions.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Full)
        assertThat(permissions.canRemovePaymentMethods).isTrue()
    }

    @Test
    fun `'createForCustomerSheet' should have payment method remove permissions of 'Partial'`() {
        val customerSheetSession = createCustomerSheetSession(PaymentMethodRemovePermission.Partial)

        val permissions = CustomerMetadata.Permissions.createForCustomerSheet(
            configuration = createCustomerSheetConfiguration(),
            customerSheetSession = customerSheetSession
        )

        assertThat(permissions.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Partial)
        assertThat(permissions.canRemovePaymentMethods).isTrue()
    }

    @Test
    fun `'createForCustomerSheet' should have payment method remove permissions of 'None'`() {
        val customerSheetSession = createCustomerSheetSession(PaymentMethodRemovePermission.None)

        val permissions = CustomerMetadata.Permissions.createForCustomerSheet(
            configuration = createCustomerSheetConfiguration(),
            customerSheetSession = customerSheetSession
        )

        assertThat(permissions.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.None)
        assertThat(permissions.canRemovePaymentMethods).isFalse()
    }

    private fun createEnabledMobilePaymentElement(
        paymentMethodRemove: ElementsSession.Customer.Components.PaymentMethodRemoveFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
        paymentMethodRemoveLast: ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
        isPaymentMethodSetAsDefaultEnabled: Boolean = true,
    ): ElementsSession.Customer.Components.MobilePaymentElement.Enabled {
        return ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
            isPaymentMethodSaveEnabled = true,
            paymentMethodRemove = paymentMethodRemove,
            paymentMethodRemoveLast = paymentMethodRemoveLast,
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

    private fun createCustomerSheetSession(
        removePermission: PaymentMethodRemovePermission
    ): CustomerSheetSession {
        val elementsSession = ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = null,
            linkSettings = null,
            orderedPaymentMethodTypesAndWallets = listOf("card"),
            customPaymentMethods = emptyList(),
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = emptyMap(),
            experimentsData = null,
            passiveCaptcha = null,
            merchantLogoUrl = null
        )

        return CustomerSheetSession(
            elementsSession = elementsSession,
            savedSelection = SavedSelection.None,
            paymentMethods = emptyList(),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            permissions = CustomerPermissions(
                removePaymentMethod = removePermission,
                canRemoveLastPaymentMethod = true,
                canUpdateFullPaymentMethodDetails = true
            ),
            defaultPaymentMethodId = null
        )
    }

    private fun createCustomerSheetConfiguration(): CustomerSheet.Configuration {
        return CustomerSheetFixtures.CONFIG_WITH_EVERYTHING
    }

    private fun customerSessionPermissionsTest(
        paymentElementDisabled: Boolean = false,
        canRemoveLastPaymentMethodConfigValue: Boolean = true,
        paymentMethodRemoveLast: ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
        paymentMethodRemove: ElementsSession.Customer.Components.PaymentMethodRemoveFeature =
            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
        block: (CustomerMetadata.Permissions) -> Unit
    ) {
        val mobilePaymentElementComponent = if (paymentElementDisabled) {
            ElementsSession.Customer.Components.MobilePaymentElement.Disabled
        } else {
            createEnabledMobilePaymentElement(
                paymentMethodRemove = paymentMethodRemove,
                paymentMethodRemoveLast = paymentMethodRemoveLast,
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
