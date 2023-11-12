package com.stripe.android.paymentsheet.prototype.paymentmethods

import android.content.res.Resources
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.prototype.AddPaymentMethodRequirement
import com.stripe.android.paymentsheet.prototype.ContactInformationCollectionMode
import com.stripe.android.paymentsheet.prototype.InitialAddPaymentMethodState
import com.stripe.android.paymentsheet.prototype.ParsingMetadata
import com.stripe.android.paymentsheet.prototype.PaymentMethodConfirmParams
import com.stripe.android.paymentsheet.prototype.PaymentMethodDefinition
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState
import com.stripe.android.paymentsheet.prototype.buildInitialState
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.format.CurrencyFormatter
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.EmbeddableImage
import com.stripe.android.uicore.text.Html

internal object AfterpayClearpayPaymentMethodDefinition : PaymentMethodDefinition {
    override val type: PaymentMethod.Type = PaymentMethod.Type.AfterpayClearpay

    override fun addRequirements(hasIntentToSetup: Boolean): Set<AddPaymentMethodRequirement> = setOf(
        AddPaymentMethodRequirement.ShippingAddress,
        AddPaymentMethodRequirement.UnsupportedForSetup,
    )

    override suspend fun initialAddState(
        metadata: ParsingMetadata
    ): InitialAddPaymentMethodState = buildInitialState(metadata) {
        val isClearpay = setOf("GB", "ES", "FR", "IT").contains(Locale.current.region)
        uiDefinition {
            selector {
                displayName = if (isClearpay) {
                    resolvableString(R.string.stripe_paymentsheet_payment_method_clearpay)
                } else {
                    resolvableString(R.string.stripe_paymentsheet_payment_method_afterpay)
                }
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_afterpay_clearpay
            }

            header(
                AfterpayHeaderUiElementDefinition(
                    isClearpay = isClearpay,
                    amount = (metadata.stripeIntent as? PaymentIntent)?.amount ?: 0L,
                    currency = metadata.stripeIntent.currency ?: "GBP",
                )
            )
            requireContactInformation(ContactInformationCollectionMode.Name)
            requireContactInformation(ContactInformationCollectionMode.Email)
            requireBillingAddress()
        }
    }

    override fun addConfirmParams(uiState: UiState.Snapshot): PaymentMethodConfirmParams {
        return PaymentMethodConfirmParams(PaymentMethodCreateParams.createAfterpayClearpay())
    }
}

private class AfterpayHeaderUiElementDefinition(
    private val isClearpay: Boolean,
    private val amount: Long,
    private val currency: String,
) : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        return true
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return AfterpayHeaderUiRenderer(
            isClearpay = isClearpay,
            htmlResolver = { resources ->
                getHtml(resources = resources)
            },
        )
    }

    private val infoUrl: String
        get() = url.format(getLocaleString(Locale.current))

    private fun getLocaleString(locale: Locale) =
        locale.language.lowercase() + "_" + locale.region.uppercase()

    private fun getHtml(resources: Resources): String {
        val numInstallments = when (currency.lowercase()) {
            "eur" -> 3
            else -> 4
        }
        return resources.getString(
            R.string.stripe_afterpay_clearpay_message
        ).replace("<num_installments/>", numInstallments.toString())
            .replace(
                "<installment_price/>",
                CurrencyFormatter.format(
                    amount / numInstallments,
                    currency
                )
            )
            // The no break space will keep the afterpay logo and (i) on the same line.
            .replace(
                "<img/>",
                "<img/>$NO_BREAK_SPACE<a href=\"$infoUrl\"><b>ⓘ</b></a>"
            ).replace("<img/>", "<img src=\"afterpay\"/>")
    }

    private companion object {
        const val url = "https://static.afterpay.com/modal/%s.html"
        const val NO_BREAK_SPACE = "\u00A0"
    }
}

private class AfterpayHeaderUiRenderer(
    private val isClearpay: Boolean,
    private val htmlResolver: (Resources) -> String,
) : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        val resources = LocalContext.current.resources
        val html = remember(htmlResolver) {
            htmlResolver(resources)
        }
        Html(
            html = html,
            enabled = enabled,
            imageLoader = mapOf(
                "afterpay" to EmbeddableImage.Drawable(
                    if (isClearpay) {
                        R.drawable.stripe_ic_clearpay_logo
                    } else {
                        R.drawable.stripe_ic_afterpay_logo
                    },
                    if (isClearpay) {
                        R.string.stripe_paymentsheet_payment_method_clearpay
                    } else {
                        R.string.stripe_paymentsheet_payment_method_afterpay
                    },
                    colorFilter = if (MaterialTheme.colors.surface.shouldUseDarkDynamicColor()) {
                        null
                    } else {
                        ColorFilter.tint(Color.White)
                    }
                )
            ),
            modifier = Modifier.padding(4.dp, 8.dp, 4.dp, 4.dp),
            color = MaterialTheme.stripeColors.subtitle,
            style = MaterialTheme.typography.h6,
            urlSpanStyle = SpanStyle(),
            imageAlign = PlaceholderVerticalAlign.Bottom
        )
    }
}
