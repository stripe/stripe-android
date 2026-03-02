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
        ) { result ->
            assertThat(result.canRemovePaymentMethods).isFalse()
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
            // Always true for `customer_session`
            assertThat(result.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should create 'Permissions' for customer session properly with remove permissions enabled`() {
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethodConfigValue = true,
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
        ) { result ->
            assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Full)
            assertThat(result.canRemovePaymentMethods).isTrue()
            assertThat(result.canRemoveLastPaymentMethod).isTrue()
            // Always true for `customer_session`
            assertThat(result.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should create 'Permissions' for customer session properly with partial remove permissions`() {
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethodConfigValue = true,
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Partial,
        ) { result ->
            assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Partial)
            assertThat(result.canRemovePaymentMethods).isTrue()
            assertThat(result.canRemoveLastPaymentMethod).isTrue()
            // Always true for `customer_session`
            assertThat(result.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should create 'Permissions' for customer session properly with disabled remove permissions`() {
        customerSessionPermissionsTest(
            canRemoveLastPaymentMethodConfigValue = true,
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled
        ) { result ->
            assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.None)
            assertThat(result.canRemovePaymentMethods).isFalse()
            assertThat(result.canRemoveLastPaymentMethod).isTrue()
            // Always true for `customer_session`
            assertThat(result.canRemoveDuplicates).isTrue()
        }
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value is true for legacy ephemeral keys`() {
        legacyEphemeralKeyTestingScenario(
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isTrue()
        }
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false for legacy ephemeral keys`() {
        legacyEphemeralKeyTestingScenario(
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
        }
    }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value & server value are false`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Disabled,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false & server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = false,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server MPE is disabled`() =
        customerSessionPermissionsTest(
            paymentElementDisabled = true,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is true but server value is false`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Disabled,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false & server value is not provided`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to false if config value is false but server value is true`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            canRemoveLastPaymentMethodConfigValue = false,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isFalse()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value is true & server value is not provided`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.NotProvided,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isTrue()
        }

    @Test
    fun `Should set 'canRemoveLastPaymentMethod' to true if config value & server value are true`() =
        customerSessionPermissionsTest(
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            canRemoveLastPaymentMethodConfigValue = true,
        ) { result ->
            assertThat(result.canRemoveLastPaymentMethod).isTrue()
        }

    @Test
    fun `Should set saveConsent to Enabled when save is enabled for customer session`() {
        customerSessionPermissionsTest { result ->
            assertThat(result.saveConsent).isEqualTo(PaymentMethodSaveConsentBehavior.Enabled)
        }
    }

    @Test
    fun `Should set saveConsent to Disabled when save is disabled for customer session`() {
        val mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
            isPaymentMethodSaveEnabled = false,
            paymentMethodRemove = ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
            paymentMethodRemoveLast = ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
            allowRedisplayOverride = PaymentMethod.AllowRedisplay.ALWAYS,
            isPaymentMethodSetAsDefaultEnabled = false,
        )
        val configuration = createConfiguration()
        val customer = createElementsSessionCustomer(
            mobilePaymentElementComponent = mobilePaymentElement,
        )
        val result = CustomerMetadata.createForPaymentSheetCustomerSession(
            configuration = configuration,
            customer = customer,
            id = "cus_test",
            ephemeralKeySecret = "ek_test",
            customerSessionClientSecret = null,
            isPaymentMethodSetAsDefaultEnabled = false,
        )
        assertThat(result.saveConsent).isEqualTo(
            PaymentMethodSaveConsentBehavior.Disabled(
                overrideAllowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
            )
        )
    }

    @Test
    fun `Should set saveConsent to Disabled when MPE is disabled for customer session`() {
        customerSessionPermissionsTest(
            paymentElementDisabled = true,
        ) { result ->
            assertThat(result.saveConsent).isEqualTo(
                PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null)
            )
        }
    }

    @Test
    fun `Should set saveConsent to Legacy for legacy ephemeral keys`() {
        legacyEphemeralKeyTestingScenario(
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) { result ->
            assertThat(result.saveConsent).isEqualTo(PaymentMethodSaveConsentBehavior.Legacy)
        }
    }

    @Test
    fun `'createForCustomerSheet' should have payment method remove permissions of 'Full'`() {
        val customerSheetSession = createCustomerSheetSession(PaymentMethodRemovePermission.Full)

        val result = CustomerMetadata.createForCustomerSheet(
            configuration = createCustomerSheetConfiguration(),
            customerSheetSession = customerSheetSession,
            id = "cus_test",
            ephemeralKeySecret = "ek_test",
            customerSessionClientSecret = null,
            isPaymentMethodSetAsDefaultEnabled = false,
        )

        assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Full)
        assertThat(result.canRemovePaymentMethods).isTrue()
    }

    @Test
    fun `'createForCustomerSheet' should have payment method remove permissions of 'Partial'`() {
        val customerSheetSession = createCustomerSheetSession(PaymentMethodRemovePermission.Partial)

        val result = CustomerMetadata.createForCustomerSheet(
            configuration = createCustomerSheetConfiguration(),
            customerSheetSession = customerSheetSession,
            id = "cus_test",
            ephemeralKeySecret = "ek_test",
            customerSessionClientSecret = null,
            isPaymentMethodSetAsDefaultEnabled = false,
        )

        assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.Partial)
        assertThat(result.canRemovePaymentMethods).isTrue()
    }

    @Test
    fun `'createForCustomerSheet' should have payment method remove permissions of 'None'`() {
        val customerSheetSession = createCustomerSheetSession(PaymentMethodRemovePermission.None)

        val result = CustomerMetadata.createForCustomerSheet(
            configuration = createCustomerSheetConfiguration(),
            customerSheetSession = customerSheetSession,
            id = "cus_test",
            ephemeralKeySecret = "ek_test",
            customerSessionClientSecret = null,
            isPaymentMethodSetAsDefaultEnabled = false,
        )

        assertThat(result.removePaymentMethod).isEqualTo(PaymentMethodRemovePermission.None)
        assertThat(result.canRemovePaymentMethods).isFalse()
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
        block: (CustomerMetadata) -> Unit
    ) {
        val result = CustomerMetadata.createForPaymentSheetLegacyEphemeralKey(
            configuration = createConfiguration(
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
            ),
            id = "cus_test",
            ephemeralKeySecret = "ek_test",
            isPaymentMethodSetAsDefaultEnabled = false,
        )

        block(result)
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
            merchantLogoUrl = null,
            elementsSessionConfigId = null,
            accountId = "acct_1SGP1sPvdtoA7EjP",
            merchantId = "acct_1SGP1sPvdtoA7EjP",
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
            defaultPaymentMethodId = null,
            customerId = "unused_for_customer_adapter_data_source",
            customerEphemeralKeySecret = "unused_for_customer_adapter_data_source",
            customerSessionClientSecret = null,
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
        block: (CustomerMetadata) -> Unit
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

        val result = CustomerMetadata.createForPaymentSheetCustomerSession(
            configuration = configuration,
            customer = customer,
            id = "cus_test",
            ephemeralKeySecret = "ek_test",
            customerSessionClientSecret = null,
            isPaymentMethodSetAsDefaultEnabled = false,
        )

        block(result)
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
