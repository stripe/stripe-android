package com.stripe.android.paymentsheet.state

import androidx.annotation.StringRes
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.BillingAddressConfig
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.DisplayablePaymentDetails
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.ButtonThemes.LinkButtonTheme
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType

internal enum class WalletLocation {
    HEADER,
    INLINE
}

internal data class WalletsState(
    private val link: Link?,
    private val googlePay: GooglePay?,
    private val shopPay: ShopPay?,
    private val walletsAllowedInHeader: List<WalletType>,
    val buttonsEnabled: Boolean,
    @StringRes val dividerTextResource: Int,
    val cardFundingFilter: CardFundingFilter,
    val cardBrandFilter: CardBrandFilter,
    val onGooglePayPressed: () -> Unit,
    val onLinkPressed: () -> Unit,
    val onShopPayPressed: () -> Unit,
) {

    fun wallets(location: WalletLocation): List<Wallet> {
        return buildList {
            googlePay(location)?.let { add(it) }
            link(location)?.let { add(it) }
            shopPay(location)?.let { add(it) }
        }
    }

    /**
     * Returns Link data if it should be displayed in the specified location, null otherwise.
     */
    fun link(location: WalletLocation): Link? {
        return when (location) {
            WalletLocation.HEADER -> link?.takeIf { walletsAllowedInHeader.contains(WalletType.Link) }
            WalletLocation.INLINE -> link?.takeUnless { walletsAllowedInHeader.contains(WalletType.Link) }
        }
    }

    /**
     * Returns GooglePay data if it should be displayed in the specified location, null otherwise.
     */
    fun googlePay(location: WalletLocation): GooglePay? {
        return when (location) {
            WalletLocation.HEADER -> googlePay?.takeIf { walletsAllowedInHeader.contains(WalletType.GooglePay) }
            WalletLocation.INLINE -> googlePay?.takeUnless { walletsAllowedInHeader.contains(WalletType.GooglePay) }
        }
    }

    /**
     * Returns ShopPay data if it should be displayed in the specified location, null otherwise.
     */
    fun shopPay(location: WalletLocation): ShopPay? {
        return when (location) {
            WalletLocation.HEADER -> null
            WalletLocation.INLINE -> shopPay
        }
    }

    val walletsInHeader
        get() = wallets(WalletLocation.HEADER).isNotEmpty()

    sealed interface Wallet

    data class Link(
        val state: LinkButtonState,
        val theme: LinkButtonTheme = LinkButtonTheme.DEFAULT,
    ) : Wallet

    data class GooglePay(
        val buttonType: GooglePayButtonType,
        val allowCreditCards: Boolean,
        val billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?,
        val additionalEnabledNetworks: List<String>
    ) : Wallet

    data object ShopPay : Wallet

    companion object {

        fun create(
            isLinkAvailable: Boolean?,
            linkEmail: String?,
            isGooglePayReady: Boolean,
            isShopPayAvailable: Boolean,
            googlePayButtonType: GooglePayButtonType,
            buttonsEnabled: Boolean,
            paymentMethodTypes: List<String>,
            googlePayLauncherConfig: GooglePayPaymentMethodLauncher.Config?,
            onGooglePayPressed: () -> Unit,
            onLinkPressed: () -> Unit,
            onShopPayPressed: () -> Unit,
            isSetupIntent: Boolean,
            walletsAllowedInHeader: List<WalletType>,
            paymentDetails: DisplayablePaymentDetails? = null,
            enableDefaultValues: Boolean = false,
            buttonThemes: PaymentSheet.ButtonThemes = PaymentSheet.ButtonThemes(
                link = LinkButtonTheme.DEFAULT
            ),
            cardFundingFilter: CardFundingFilter,
            cardBrandFilter: CardBrandFilter
        ): WalletsState? {
            val link = createLink(
                isLinkAvailable = isLinkAvailable,
                linkEmail = linkEmail,
                paymentDetails = paymentDetails,
                enableDefaultValues = enableDefaultValues,
                buttonThemes = buttonThemes
            )

            val googlePay = createGooglePay(
                isGooglePayReady = isGooglePayReady,
                googlePayButtonType = googlePayButtonType,
                googlePayLauncherConfig = googlePayLauncherConfig
            )

            val shopPay = ShopPay.takeIf { isShopPayAvailable }

            return if (link != null || googlePay != null || shopPay != null) {
                WalletsState(
                    link = link,
                    googlePay = googlePay,
                    shopPay = shopPay,
                    buttonsEnabled = buttonsEnabled,
                    dividerTextResource = getDividerTextResource(paymentMethodTypes, isSetupIntent),
                    onGooglePayPressed = onGooglePayPressed,
                    onLinkPressed = onLinkPressed,
                    onShopPayPressed = onShopPayPressed,
                    walletsAllowedInHeader = walletsAllowedInHeader,
                    cardFundingFilter = cardFundingFilter,
                    cardBrandFilter = cardBrandFilter
                )
            } else {
                null
            }
        }

        private fun createLink(
            isLinkAvailable: Boolean?,
            linkEmail: String?,
            paymentDetails: DisplayablePaymentDetails?,
            enableDefaultValues: Boolean,
            buttonThemes: PaymentSheet.ButtonThemes
        ): Link? {
            return if (isLinkAvailable == true) {
                Link(
                    state = LinkButtonState.create(
                        linkEmail = linkEmail,
                        paymentDetails = paymentDetails,
                        enableDefaultValues = enableDefaultValues
                    ),
                    theme = buttonThemes.link
                )
            } else {
                null
            }
        }

        private fun createGooglePay(
            isGooglePayReady: Boolean,
            googlePayButtonType: GooglePayButtonType,
            googlePayLauncherConfig: GooglePayPaymentMethodLauncher.Config?
        ): GooglePay? {
            return GooglePay(
                allowCreditCards = googlePayLauncherConfig?.allowCreditCards ?: false,
                buttonType = googlePayButtonType,
                additionalEnabledNetworks = googlePayLauncherConfig?.additionalEnabledNetworks.orEmpty(),
                billingAddressParameters = googlePayLauncherConfig?.let {
                    GooglePayJsonFactory.BillingAddressParameters(
                        isRequired = it.billingAddressConfig.isRequired,
                        format = when (it.billingAddressConfig.format) {
                            BillingAddressConfig.Format.Min -> {
                                GooglePayJsonFactory.BillingAddressParameters.Format.Min
                            }
                            BillingAddressConfig.Format.Full -> {
                                GooglePayJsonFactory.BillingAddressParameters.Format.Full
                            }
                        },
                        isPhoneNumberRequired = it.billingAddressConfig.isPhoneNumberRequired,
                    )
                },
            ).takeIf { isGooglePayReady }
        }

        @StringRes
        private fun getDividerTextResource(
            paymentMethodTypes: List<String>,
            isSetupIntent: Boolean
        ): Int {
            return if (paymentMethodTypes.singleOrNull() == Card.code && !isSetupIntent) {
                R.string.stripe_paymentsheet_or_pay_with_card
            } else if (paymentMethodTypes.singleOrNull() == null && !isSetupIntent) {
                R.string.stripe_paymentsheet_or_pay_using
            } else if (paymentMethodTypes.singleOrNull() == Card.code && isSetupIntent) {
                R.string.stripe_paymentsheet_or_use_a_card
            } else {
                R.string.stripe_paymentsheet_or_use
            }
        }
    }
}
