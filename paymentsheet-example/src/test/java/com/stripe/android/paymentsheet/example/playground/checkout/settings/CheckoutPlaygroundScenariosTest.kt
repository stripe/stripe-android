@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.LinkType
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import org.junit.Test

class CheckoutPlaygroundScenariosTest {
    @Test
    fun `every catalog leaf produces a valid snapshot`() {
        CheckoutPlaygroundScenarios.leaves.forEach { leaf ->
            val settings = CheckoutPlaygroundSettings.createInMemory()

            settings.applyPreset(leaf.preset)

            assertThat(settings.validationErrors()).isEmpty()
            settings.snapshot()
        }
    }

    @Test
    fun `catalog paths are unique`() {
        val paths = CheckoutPlaygroundScenarios.root.leafPaths()

        assertThat(paths).containsNoDuplicates()
        assertThat(paths).hasSize(CheckoutPlaygroundScenarios.leaves.size)
    }

    @Test
    fun `US common contains expected typed regional values`() {
        val snapshot = snapshotFor("us_common")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.merchant]).isEqualTo(Merchant.US)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.currency]).isEqualTo(Currency.USD)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.automaticPaymentMethods]).isFalse()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.paymentMethodTypes]).containsExactly(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.USBankAccount.code,
            PaymentMethod.Type.Link.code,
            PaymentMethod.Type.CashAppPay.code,
            PaymentMethod.Type.Klarna.code,
        ).inOrder()
    }

    @Test
    fun `Japan contains expected typed regional values`() {
        val snapshot = snapshotFor("japan", parentKey = "asia_pacific")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.merchant]).isEqualTo(Merchant.JP)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.currency]).isEqualTo(Currency.JPY)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.paymentMethodTypes]).containsExactly(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.Konbini.code,
            PaymentMethod.Type.PayPay.code,
        ).inOrder()
    }

    @Test
    fun `returning customer scenario does not reuse customer from another merchant`() {
        val settings = CheckoutPlaygroundSettings.createInMemory().apply {
            saveReturningCustomer("cus_from_another_merchant")
        }
        val returningCustomer = CheckoutPlaygroundScenarios.groups
            .single { it.key == "returning_customer" }
        val saveAndRemove = returningCustomer.children
            .filterIsInstance<CheckoutPlaygroundScenario.Leaf>()
            .single { it.key == "save_remove" }

        settings.applyPreset(saveAndRemove.preset)
        assertThat(settings[CheckoutPlaygroundDefinitions.session.customerId]).isNull()
        assertThat(settings[CheckoutPlaygroundDefinitions.session.customer]).isEqualTo(CheckoutCustomer.Returning)
    }

    @Test
    fun `ECE catalog contains 15 scenarios`() {
        val ece = CheckoutPlaygroundScenarios.groups.single { it.key == "ece" }

        assertThat(ece.children).hasSize(15)
    }

    @Test
    fun `ECE scenarios hide Link and Google Pay in Payment Element`() {
        val ece = CheckoutPlaygroundScenarios.groups.single { it.key == "ece" }

        ece.children.filterIsInstance<CheckoutPlaygroundScenario.Leaf>().forEach { leaf ->
            val snapshot = CheckoutPlaygroundSettings.createInMemory().apply {
                applyPreset(leaf.preset)
            }.snapshot()

            assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.payment.link.display])
                .isEqualTo(PaymentElement.Configuration.LinkConfiguration.Display.Never)
            assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.payment.googlePay.display])
                .isEqualTo(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
        }
    }

    @Test
    fun `default ECE scenario sets only the element configuration`() {
        val snapshot = snapshotFor("default", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.shouldSetConfiguration]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.link.display])
            .isEqualTo(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.googlePay.display])
            .isEqualTo(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Automatic)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.appearance.theme])
            .isEqualTo(ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Automatic)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.paymentMethodOrder]).isEmpty()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.automaticTax]).isFalse()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.shippingAddressCollection]).isFalse()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.billingAddressCollection]).isFalse()
    }

    @Test
    fun `ECE scenarios cover every Link type`() {
        val ece = CheckoutPlaygroundScenarios.groups.single { it.key == "ece" }
        val linkTypes = ece.children.filterIsInstance<CheckoutPlaygroundScenario.Leaf>().map { leaf ->
            CheckoutPlaygroundSettings.createInMemory().apply {
                applyPreset(leaf.preset)
            }.snapshot()[CheckoutPlaygroundDefinitions.session.linkType]
        }

        assertThat(linkTypes).containsAtLeastElementsIn(LinkType.entries)
    }

    @Test
    fun `hidden ECE scenario omits the element configuration`() {
        val snapshot = snapshotFor("hidden", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.shouldSetConfiguration]).isFalse()
    }

    @Test
    fun `custom Google Pay scenario covers Google Pay and top-level configuration`() {
        val snapshot = snapshotFor("custom_google_pay", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.googlePay.label])
            .isEqualTo("ECE total")
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.googlePay.buttonType])
            .isEqualTo(ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Checkout)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.googlePay.additionalNetworks])
            .containsExactly("INTERAC")
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.merchantDisplayName]).isEqualTo("ECE Store")
    }

    @Test
    fun `restricted Link scenario covers Link options and web Link`() {
        val snapshot = snapshotFor("restricted_link", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.googlePay.display])
            .isEqualTo(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.link.collectMissingBilling]).isFalse()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.link.disallowedFunding])
            .containsExactly("card", "bank_account").inOrder()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.billingAddressCollection]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.linkType]).isEqualTo(LinkType.Web)
    }

    @Test
    fun `single slot scenario combines appearance and Link-first ordering`() {
        val snapshot = snapshotFor("link_first_slot", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.appearance.theme])
            .isEqualTo(ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Light)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.appearance.layout.columns]).isEqualTo(1)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.appearance.layout.rows]).isEqualTo(1)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.express.paymentMethodOrder])
            .containsExactly("link", "google_pay").inOrder()
    }

    @Test
    fun `auto tax shipping scenario requires shipping but not billing`() {
        val snapshot = snapshotFor("auto_tax_shipping", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.automaticTax]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.shippingAddressCollection]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.billingAddressCollection]).isFalse()
    }

    @Test
    fun `auto tax billing scenario requires billing but not shipping`() {
        val snapshot = snapshotFor("auto_tax_billing", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.automaticTax]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.billingAddressCollection]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.shippingAddressCollection]).isFalse()
    }

    @Test
    fun `missing email scenario has no Checkout Session or default email`() {
        val snapshot = snapshotFor("missing_email", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.customerEmail]).isEmpty()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.defaults.email]).isNull()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.billingAddressCollection]).isFalse()
    }

    @Test
    fun `prefilled defaults scenario supplies contact details`() {
        val snapshot = snapshotFor("prefilled_defaults", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.customerEmail]).isEmpty()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.defaults.email])
            .isEqualTo("jenny.rosen@example.com")
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.defaults.billing.enabled]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.defaults.shipping.enabled]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.billingAddressCollection]).isTrue()
        assertThat(snapshot[CheckoutPlaygroundDefinitions.session.shippingAddressCollection]).isTrue()
    }

    @Test
    fun `all elements hides Link and Google Pay in Payment Element`() {
        val snapshot = snapshotFor("all_elements", parentKey = "elements")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.payment.link.display])
            .isEqualTo(PaymentElement.Configuration.LinkConfiguration.Display.Never)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.payment.googlePay.display])
            .isEqualTo(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
    }

    private fun snapshotFor(
        key: String,
        parentKey: String? = null,
    ): CheckoutPlaygroundSettings.Snapshot {
        val parent = parentKey?.let { requestedKey ->
            CheckoutPlaygroundScenarios.groups.single { it.key == requestedKey }
        }
        val leaf = (parent?.children ?: CheckoutPlaygroundScenarios.leaves)
            .filterIsInstance<CheckoutPlaygroundScenario.Leaf>()
            .single { it.key == key }
        return CheckoutPlaygroundSettings.createInMemory().apply {
            applyPreset(leaf.preset)
        }.snapshot()
    }
}

private fun CheckoutPlaygroundScenario.Group.leafPaths(
    parents: List<String> = emptyList(),
): List<String> {
    val path = parents + key
    return children.flatMap { child ->
        when (child) {
            is CheckoutPlaygroundScenario.Group -> child.leafPaths(path)
            is CheckoutPlaygroundScenario.Leaf -> listOf((path + child.key).joinToString("/"))
        }
    }
}
