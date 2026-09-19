package com.stripe.android.lpmfoundations.paymentmethod.definitions

import android.content.Context
import android.text.style.URLSpan
import androidx.core.text.HtmlCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.MandateTextElement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class NgCardDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.NgCard),
        )

        assertThat(NgCardDefinition.isSupported(metadata)).isEqualTo(true)
        assertThat(NgCardDefinition.requiresMandate(metadata)).isEqualTo(intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent)
    }

    @Test
    fun `form matches web instructions and mandate`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.NgCard),
        )
        val formElements = NgCardDefinition.formElements(metadata)

        val notice = formElements.single() as MandateTextElement
        assertThat(notice.stringResId).isEqualTo(R.string.stripe_ng_payment_mor_notice)
        assertThat(notice.args).containsExactly(NIGERIAN_PAYMENT_METHOD_TERMS_URL)
    }

    @Test
    fun `terms display never hides terms without changing confirmation requirements`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent
                .stripeIntent(PaymentMethod.Type.NgCard),
            termsDisplay = mapOf(PaymentMethod.Type.NgCard to PaymentSheet.TermsDisplay.NEVER),
        )
        val formElements = NgCardDefinition.formElements(metadata)

        assertThat(formElements).isEmpty()
        assertThat(NgCardDefinition.requiresMandate(metadata)).isEqualTo(true)
    }

    @Test
    fun `collects configured contact information`() {
        val formElements = NgCardDefinition.formElements(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(paymentMethodTypes = listOf("ng_card")),
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
            )
        )

        assertThat(formElements).hasSize(3)
        checkPhoneField(formElements, 0)
        checkEmailField(formElements, 1)
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(NgCardDefinition.supportedAsSavedPaymentMethod).isFalse()
    }

    @Test
    fun `renders the merchant of record notice with a usable terms URL`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notice = context.getString(R.string.stripe_ng_payment_mor_notice, NIGERIAN_PAYMENT_METHOD_TERMS_URL)

        assertThat(notice).contains("Global Stack Services Limited as merchant of record")
        val renderedNotice = HtmlCompat.fromHtml(notice, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val links = renderedNotice.getSpans(0, renderedNotice.length, URLSpan::class.java)
        assertThat(links.map { it.url }).containsExactly(NIGERIAN_PAYMENT_METHOD_TERMS_URL)
        assertThat(notice).doesNotContain("%s")
    }
}
