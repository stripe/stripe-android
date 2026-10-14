package com.stripe.android.lpmfoundations.paymentmethod.definitions

import android.content.Context
import android.text.style.URLSpan
import androidx.core.text.HtmlCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.isSupported
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class NgUssdDefinitionTest {
    @Test
    fun `supports the documented intent modes`(
        @TestParameter intentScenario: LpmBillingAddressTestConfiguration.IntentScenario,
    ) {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = intentScenario.stripeIntent(PaymentMethod.Type.NgUssd),
        )

        assertThat(NgUssdDefinition.isSupported(metadata)).isEqualTo(
            intentScenario == LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
        )
        assertThat(NgUssdDefinition.requiresMandate(metadata)).isFalse()
    }

    @Test
    fun `does not redisplay saved payment methods`() {
        assertThat(NgUssdDefinition.supportedAsSavedPaymentMethod).isFalse()
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
