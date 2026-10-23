package com.stripe.android.elements

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeThemeDefaults
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class ExpressCheckoutElementTest {
    @Test
    fun `configuration builds default values`() {
        val state = ExpressCheckoutElement.Configuration().build()

        assertThat(state.linkConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Automatic
        )
        assertThat(state.googlePayConfiguration.display).isEqualTo(
            CheckoutGooglePayConfiguration.Display.Automatic
        )
        assertThat(state.googlePayConfiguration.label).isNull()
        assertThat(state.googlePayConfiguration.buttonType).isEqualTo(
            PaymentSheet.GooglePayConfiguration.ButtonType.Pay
        )
        assertThat(state.googlePayConfiguration.additionalEnabledNetworks).isEmpty()
        assertThat(state.appearance.buttonLayout.maxColumns).isNull()
        assertThat(state.appearance.buttonLayout.maxRows).isNull()
        assertThat(state.appearance.buttonTheme).isEqualTo(
            ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Automatic
        )
        assertThat(state.appearance.cornerRadiusDp)
            .isEqualTo(StripeThemeDefaults.shapes.cornerRadius)
    }

    @Test
    fun `configuration builds requested values`() {
        val state = ExpressCheckoutElement.Configuration()
            .linkConfiguration(
                ExpressCheckoutElement.Configuration.LinkConfiguration()
                    .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
            )
            .googlePayConfiguration(
                ExpressCheckoutElement.Configuration.GooglePayConfiguration()
                    .display(ExpressCheckoutElement.Configuration.GooglePayConfiguration.Display.Never)
                    .label("Complete your purchase")
                    .buttonType(ExpressCheckoutElement.Configuration.GooglePayConfiguration.ButtonType.Checkout)
                    .additionalEnabledNetworks(listOf("INTERAC"))
            )
            .build()

        assertThat(state.linkConfiguration.display).isEqualTo(
            ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never
        )
        assertThat(state.googlePayConfiguration.display).isEqualTo(
            CheckoutGooglePayConfiguration.Display.Never
        )
        assertThat(state.googlePayConfiguration.label).isEqualTo("Complete your purchase")
        assertThat(state.googlePayConfiguration.buttonType).isEqualTo(
            PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
        )
        assertThat(state.googlePayConfiguration.additionalEnabledNetworks).containsExactly("INTERAC")
    }

    @Test
    fun `configuration builds requested appearance`() {
        val state = ExpressCheckoutElement.Configuration()
            .appearance(
                ExpressCheckoutElement.Configuration.Appearance()
                    .buttonLayout(
                        ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                            .maxColumns(2)
                            .maxRows(1)
                    )
                    .buttonTheme(ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Light)
                    .cornerRadiusDp(12f)
            )
            .build()

        assertThat(state.appearance.buttonLayout.maxColumns).isEqualTo(2)
        assertThat(state.appearance.buttonLayout.maxRows).isEqualTo(1)
        assertThat(state.appearance.buttonTheme).isEqualTo(
            ExpressCheckoutElement.Configuration.Appearance.ButtonTheme.Light
        )
        assertThat(state.appearance.cornerRadiusDp).isEqualTo(12f)
    }

    @Test
    fun `appearance builds corner radius from dimension resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val state = ExpressCheckoutElement.Configuration.Appearance()
            .cornerRadiusDp(context, R.dimen.stripe_paymentsheet_outer_spacing_top)
            .build()

        assertThat(state.cornerRadiusDp).isEqualTo(10f)
    }

    @Test
    fun `button layout rejects non-positive maximum columns`() {
        val exception = runCatching {
            ExpressCheckoutElement.Configuration.Appearance.ButtonLayout().maxColumns(0)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `button layout rejects maximum columns greater than ten`() {
        val exception = runCatching {
            ExpressCheckoutElement.Configuration.Appearance.ButtonLayout().maxColumns(11)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `button layout rejects non-positive maximum rows`() {
        val exception = runCatching {
            ExpressCheckoutElement.Configuration.Appearance.ButtonLayout().maxRows(-1)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `button layout rejects maximum rows greater than ten`() {
        val exception = runCatching {
            ExpressCheckoutElement.Configuration.Appearance.ButtonLayout().maxRows(11)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
