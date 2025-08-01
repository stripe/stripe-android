package com.stripe.android.paymentsheet.model

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.model.LinkMode
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.ach.BankFormScreenState
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountTextBuilder
import com.stripe.android.paymentsheet.ui.createCardLabel
import com.stripe.android.paymentsheet.ui.getCardBrandIcon
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getLinkIcon
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

internal sealed class PaymentSelection : Parcelable {

    var hasAcknowledgedSepaMandate: Boolean = false

    abstract val requiresConfirmation: Boolean

    abstract fun mandateText(
        merchantName: String,
        isSetupFlow: Boolean,
    ): ResolvableString?

    @Parcelize
    data object GooglePay : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
            return null
        }
    }

    @Parcelize
    data class Link(
        val useLinkExpress: Boolean = false,
        val selectedPayment: LinkPaymentMethod? = null,
        val shippingAddress: ConsumerShippingAddress? = null,
    ) : PaymentSelection() {

        override val requiresConfirmation: Boolean
            get() = false

        val billingDetails: PaymentMethod.BillingDetails?
            get() = selectedPayment?.let {
                val billingAddress = it.details.billingAddress
                PaymentMethod.BillingDetails(
                    address = Address(
                        city = billingAddress?.locality,
                        country = billingAddress?.countryCode?.value,
                        line1 = billingAddress?.line1,
                        line2 = billingAddress?.line2,
                        postalCode = billingAddress?.postalCode,
                        state = billingAddress?.administrativeArea,
                    ),
                    email = it.details.billingEmailAddress,
                    phone = it.billingPhone,
                    name = billingAddress?.name,
                )
            }

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
            return null
        }
    }

    @Parcelize
    data object ShopPay : PaymentSelection() {
        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(merchantName: String, isSetupFlow: Boolean) = null
    }

    @Parcelize
    data class ExternalPaymentMethod(
        val type: String,
        val billingDetails: PaymentMethod.BillingDetails?,
        val label: ResolvableString,
        // In practice, we don't have an iconResource for external payment methods.
        @DrawableRes val iconResource: Int,
        // In practice, we always have a lightThemeIconUrl for external payment methods.
        val lightThemeIconUrl: String?,
        val darkThemeIconUrl: String?,
    ) : PaymentSelection() {
        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean
        ): ResolvableString? {
            return null
        }
    }

    @Parcelize
    data class CustomPaymentMethod(
        val id: String,
        val billingDetails: PaymentMethod.BillingDetails?,
        val label: ResolvableString,
        val lightThemeIconUrl: String?,
        val darkThemeIconUrl: String?,
    ) : PaymentSelection() {
        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean
        ): ResolvableString? {
            return null
        }
    }

    @Parcelize
    data class Saved(
        val paymentMethod: PaymentMethod,
        val walletType: WalletType? = null,
        val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
    ) : PaymentSelection() {

        enum class WalletType(val paymentSelection: PaymentSelection) {
            GooglePay(PaymentSelection.GooglePay), Link(Link())
        }

        val showMandateAbovePrimaryButton: Boolean
            get() {
                return paymentMethod.type == PaymentMethod.Type.SepaDebit
            }

        override val requiresConfirmation: Boolean
            get() = paymentMethod.type == USBankAccount ||
                paymentMethod.type == PaymentMethod.Type.SepaDebit

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
            return when (paymentMethod.type) {
                USBankAccount -> {
                    USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
                        merchantName = merchantName,
                        isVerifyingMicrodeposits = false,
                        isSaveForFutureUseSelected = false,
                        isInstantDebits = false,
                        isSetupFlow = isSetupFlow,
                    )
                }
                PaymentMethod.Type.SepaDebit -> {
                    resolvableString(StripeUiCoreR.string.stripe_sepa_mandate, merchantName)
                }
                else -> {
                    null
                }
            }
        }
    }

    enum class CustomerRequestedSave(val setupFutureUsage: ConfirmPaymentIntentParams.SetupFutureUsage?) {
        RequestReuse(ConfirmPaymentIntentParams.SetupFutureUsage.OffSession),
        RequestNoReuse(ConfirmPaymentIntentParams.SetupFutureUsage.Blank),
        NoRequest(null)
    }

    sealed class New : PaymentSelection() {

        abstract val paymentMethodCreateParams: PaymentMethodCreateParams
        abstract val paymentMethodOptionsParams: PaymentMethodOptionsParams?
        abstract val paymentMethodExtraParams: PaymentMethodExtraParams?
        abstract val customerRequestedSave: CustomerRequestedSave

        override val requiresConfirmation: Boolean
            get() = false

        override fun mandateText(
            merchantName: String,
            isSetupFlow: Boolean,
        ): ResolvableString? {
            return null
        }

        @Parcelize
        data class Card(
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            val brand: CardBrand,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        ) : New() {
            @IgnoredOnParcel
            val last4: String = paymentMethodCreateParams.cardLast4().orEmpty()
        }

        @Parcelize
        data class USBankAccount(
            val label: String,
            @DrawableRes val iconResource: Int,
            val input: Input,
            val screenState: BankFormScreenState,
            val instantDebits: InstantDebitsInfo?,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        ) : New() {

            override fun mandateText(
                merchantName: String,
                isSetupFlow: Boolean,
            ): ResolvableString? {
                return screenState.linkedBankAccount?.mandateText
            }

            @Parcelize
            data class InstantDebitsInfo(
                val paymentMethod: PaymentMethod,
                val linkMode: LinkMode?,
            ) : Parcelable

            @Parcelize
            data class Input(
                val name: String,
                val email: String?,
                val phone: String?,
                val address: Address?,
                val saveForFutureUse: Boolean,
            ) : Parcelable
        }

        @Parcelize
        data class LinkInline(
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            val brand: CardBrand,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
            val input: UserInput,
        ) : New() {
            @IgnoredOnParcel
            val last4: String = paymentMethodCreateParams.cardLast4().orEmpty()
        }

        @Parcelize
        data class GenericPaymentMethod(
            val label: ResolvableString,
            @DrawableRes val iconResource: Int,
            val lightThemeIconUrl: String?,
            val darkThemeIconUrl: String?,
            override val paymentMethodCreateParams: PaymentMethodCreateParams,
            override val customerRequestedSave: CustomerRequestedSave,
            override val paymentMethodOptionsParams: PaymentMethodOptionsParams? = null,
            override val paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        ) : New()
    }

    class IconLoader @Inject constructor(
        private val resources: Resources,
        private val imageLoader: StripeImageLoader,
    ) {
        private fun isDarkTheme(): Boolean {
            return resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }

        suspend fun load(
            @DrawableRes drawableResourceId: Int,
            lightThemeIconUrl: String?,
            darkThemeIconUrl: String?,
        ): Drawable {
            fun loadResource(): Drawable {
                @Suppress("DEPRECATION")
                return runCatching {
                    ResourcesCompat.getDrawable(
                        resources,
                        drawableResourceId,
                        null
                    )
                }.getOrNull() ?: emptyDrawable
            }

            suspend fun loadIcon(url: String): Drawable {
                return imageLoader.load(url).getOrNull()?.let {
                    BitmapDrawable(resources, it)
                } ?: loadResource()
            }

            // If the payment option has an icon URL, we prefer it.
            // Some payment options don't have an icon URL, and are loaded locally via resource.
            return if (isDarkTheme() && darkThemeIconUrl != null) {
                loadIcon(darkThemeIconUrl)
            } else if (lightThemeIconUrl != null) {
                loadIcon(lightThemeIconUrl)
            } else {
                loadResource()
            }
        }

        companion object {
            @VisibleForTesting
            val emptyDrawable = ShapeDrawable()
        }
    }
}

internal val PaymentSelection.isLink: Boolean
    get() = when (this) {
        is PaymentSelection.GooglePay -> false
        is PaymentSelection.Link -> true
        is PaymentSelection.New.LinkInline -> true
        is PaymentSelection.New -> false
        is PaymentSelection.Saved -> walletType == PaymentSelection.Saved.WalletType.Link
        is PaymentSelection.CustomPaymentMethod,
        is PaymentSelection.ExternalPaymentMethod -> false
        is PaymentSelection.ShopPay -> false
    }

internal val PaymentSelection.isSaved: Boolean
    get() = when (this) {
        is PaymentSelection.Saved -> true
        else -> false
    }

internal val PaymentSelection.drawableResourceId: Int
    get() = when (this) {
        is PaymentSelection.ExternalPaymentMethod -> iconResource
        is PaymentSelection.CustomPaymentMethod -> 0
        PaymentSelection.GooglePay -> R.drawable.stripe_google_pay_mark
        is PaymentSelection.Link -> getLinkIcon(iconOnly = true)
        is PaymentSelection.New.Card -> brand.getCardBrandIcon()
        is PaymentSelection.New.GenericPaymentMethod -> iconResource
        is PaymentSelection.New.LinkInline -> brand.getCardBrandIcon()
        is PaymentSelection.New.USBankAccount -> iconResource
        is PaymentSelection.Saved -> getSavedIcon(this)
        is PaymentSelection.ShopPay -> R.drawable.stripe_shop_pay_logo_white
    }

private fun getSavedIcon(selection: PaymentSelection.Saved): Int {
    if (selection.paymentMethod.isLinkCardBrand) {
        return R.drawable.stripe_ic_paymentsheet_link_arrow
    }

    return when (val resourceId = selection.paymentMethod.getSavedPaymentMethodIcon()) {
        R.drawable.stripe_ic_paymentsheet_card_unknown_ref -> {
            when (selection.walletType) {
                PaymentSelection.Saved.WalletType.Link -> getLinkIcon()
                PaymentSelection.Saved.WalletType.GooglePay -> R.drawable.stripe_google_pay_mark
                else -> resourceId
            }
        }
        else -> resourceId
    }
}

internal val PaymentSelection.lightThemeIconUrl: String?
    get() = when (this) {
        is PaymentSelection.ExternalPaymentMethod -> lightThemeIconUrl
        is PaymentSelection.CustomPaymentMethod -> lightThemeIconUrl
        PaymentSelection.GooglePay -> null
        is PaymentSelection.Link -> null
        is PaymentSelection.New.Card -> null
        is PaymentSelection.New.GenericPaymentMethod -> lightThemeIconUrl
        is PaymentSelection.New.LinkInline -> null
        is PaymentSelection.New.USBankAccount -> null
        is PaymentSelection.Saved -> null
        is PaymentSelection.ShopPay -> null
    }

internal val PaymentSelection.darkThemeIconUrl: String?
    get() = when (this) {
        is PaymentSelection.ExternalPaymentMethod -> darkThemeIconUrl
        is PaymentSelection.CustomPaymentMethod -> darkThemeIconUrl
        PaymentSelection.GooglePay -> null
        is PaymentSelection.Link -> null
        is PaymentSelection.New.Card -> null
        is PaymentSelection.New.GenericPaymentMethod -> darkThemeIconUrl
        is PaymentSelection.New.LinkInline -> null
        is PaymentSelection.New.USBankAccount -> null
        is PaymentSelection.Saved -> null
        is PaymentSelection.ShopPay -> null
    }

internal val PaymentSelection.label: ResolvableString
    get() = when (this) {
        is PaymentSelection.ExternalPaymentMethod -> label
        is PaymentSelection.CustomPaymentMethod -> label
        PaymentSelection.GooglePay -> StripeR.string.stripe_google_pay.resolvableString
        is PaymentSelection.Link -> StripeR.string.stripe_link.resolvableString
        is PaymentSelection.New.Card -> createCardLabel(last4).orEmpty()
        is PaymentSelection.New.GenericPaymentMethod -> label
        is PaymentSelection.New.LinkInline -> createCardLabel(last4).orEmpty()
        is PaymentSelection.New.USBankAccount -> label.resolvableString
        is PaymentSelection.Saved -> getSavedLabel(this).orEmpty()
        is PaymentSelection.ShopPay -> StripeR.string.stripe_shop_pay.resolvableString
    }

private fun getSavedLabel(selection: PaymentSelection.Saved): ResolvableString? {
    return selection.paymentMethod.getLabel(canShowSublabel = true) ?: run {
        when (selection.walletType) {
            PaymentSelection.Saved.WalletType.Link -> StripeR.string.stripe_link.resolvableString
            PaymentSelection.Saved.WalletType.GooglePay -> StripeR.string.stripe_google_pay.resolvableString
            else -> null
        }
    }
}

internal val PaymentSelection.paymentMethodType: String
    get() = when (this) {
        is PaymentSelection.ExternalPaymentMethod -> type
        is PaymentSelection.CustomPaymentMethod -> id
        PaymentSelection.GooglePay -> "google_pay"
        is PaymentSelection.Link -> "link"
        is PaymentSelection.New -> paymentMethodCreateParams.typeCode
        is PaymentSelection.Saved -> paymentMethod.type?.code ?: "card"
        is PaymentSelection.ShopPay -> "shop_pay"
    }

internal val PaymentSelection.billingDetails: PaymentMethod.BillingDetails?
    get() = when (this) {
        is PaymentSelection.ExternalPaymentMethod -> billingDetails
        is PaymentSelection.CustomPaymentMethod -> billingDetails
        PaymentSelection.GooglePay -> null
        is PaymentSelection.Link -> billingDetails
        is PaymentSelection.New -> paymentMethodCreateParams.billingDetails
        is PaymentSelection.Saved -> paymentMethod.billingDetails
        is PaymentSelection.ShopPay -> null
    }

internal fun PaymentMethod.BillingDetails.toPaymentSheetBillingDetails(): PaymentSheet.BillingDetails {
    return PaymentSheet.BillingDetails(
        address = PaymentSheet.Address(
            city = address?.city,
            country = address?.country,
            line1 = address?.line1,
            line2 = address?.line2,
            postalCode = address?.postalCode,
            state = address?.state
        ),
        email = email,
        name = name,
        phone = phone
    )
}

internal fun PaymentSelection.Saved.mandateTextFromPaymentMethodMetadata(
    metadata: PaymentMethodMetadata
): ResolvableString? = mandateText(
    metadata.merchantName,
    metadata.hasIntentToSetup(paymentMethod.type?.code ?: "")
)

/**
 * If setup_future_usage is set at the top level to "off_session" the payment method will
 * inherit this value so sending [ConfirmPaymentIntentParams.SetupFutureUsage.OnSession] and
 * [ConfirmPaymentIntentParams.SetupFutureUsage.Blank] have no effect.
 * If setup_future_usage is set to "on_session" at either the top level or payment_method_options level it can
 * only be updated to "off_session". This method will always return the [ConfirmPaymentIntentParams.SetupFutureUsage]
 * value if it is [ConfirmPaymentIntentParams.SetupFutureUsage.OffSession]. Otherwise it will return null if
 * @param hasIntentToSetup is true
 */
internal fun PaymentSelection.CustomerRequestedSave.getSetupFutureUseValue(
    hasIntentToSetup: Boolean
): ConfirmPaymentIntentParams.SetupFutureUsage? {
    return when (setupFutureUsage) {
        ConfirmPaymentIntentParams.SetupFutureUsage.OffSession -> setupFutureUsage
        else -> setupFutureUsage.takeIf { !hasIntentToSetup }
    }
}

private val PaymentMethod.isLinkCardBrand: Boolean
    get() = type == PaymentMethod.Type.Card && linkPaymentDetails is LinkPaymentDetails.BankAccount
