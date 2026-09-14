@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.PaymentElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.checkout.CheckoutControllerExampleRequestFactory
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import kotlinx.serialization.json.JsonPrimitive
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
        val request = CheckoutControllerExampleRequestFactory.create(settings.snapshot())

        assertThat(settings[CheckoutPlaygroundDefinitions.session.customerId]).isNull()
        assertThat(request.body).containsEntry("customer", JsonPrimitive("returning"))
    }

    @Test
    fun `ECE scenarios hide Link and Google Pay in Payment Element`() {
        val snapshot = snapshotFor("link_google_pay", parentKey = "ece")

        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.payment.link.display])
            .isEqualTo(PaymentElement.Configuration.LinkConfiguration.Display.Never)
        assertThat(snapshot[CheckoutPlaygroundDefinitions.Controller.payment.googlePay.display])
            .isEqualTo(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
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
