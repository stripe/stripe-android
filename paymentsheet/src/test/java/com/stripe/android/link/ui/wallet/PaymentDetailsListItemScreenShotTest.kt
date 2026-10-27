package com.stripe.android.link.ui.wallet

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.image.LocalStripeImageLoader
import com.stripe.android.utils.FakeStripeImageLoader
import org.junit.Rule
import org.junit.Test

internal class PaymentDetailsListItemScreenShotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testCardEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = ConsumerPaymentDetails.Card.Funding.Credit,
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    ),
                    display = null,
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testCardEnabledAndSelected() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = ConsumerPaymentDetails.Card.Funding.Credit,
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    ),
                    display = null,
                ),
                enabled = true,
                isSelected = true,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testCardEnabledAndUpdating() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = ConsumerPaymentDetails.Card.Funding.Credit,
                    nickname = "My Personal Card",
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    ),
                    display = null,
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = true
            )
        )
    }

    @Test
    fun testCardDisabledAndSelected() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = ConsumerPaymentDetails.Card.Funding.Credit,
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    ),
                    display = null,
                ),
                enabled = false,
                isSelected = true,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testCardEnabledAndUnavailable() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Card(
                    id = "QAAAKJ6",
                    expiryYear = 2023,
                    expiryMonth = 12,
                    isDefault = true,
                    brand = CardBrand.MasterCard,
                    last4 = "4444",
                    cvcCheck = CvcCheck.Pass,
                    networks = emptyList(),
                    funding = ConsumerPaymentDetails.Card.Funding.Credit,
                    nickname = null,
                    billingAddress = ConsumerPaymentDetails.BillingAddress(
                        name = null,
                        line1 = null,
                        line2 = null,
                        locality = null,
                        administrativeArea = null,
                        countryCode = CountryCode.US,
                        postalCode = "12312"
                    ),
                    display = null,
                ),
                enabled = true,
                isSelected = true,
                isAvailable = false,
                isUpdating = false
            )
        )
    }

    @Test
    fun testBankAccountEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.BankAccount(
                    id = "wAAACGA",
                    last4 = "6789",
                    bankAccountName = "STRIPE TEST BANK ACCOUNT",
                    bankIconCode = null,
                    isDefault = false,
                    nickname = null,
                    billingAddress = null,
                    billingEmailAddress = null,
                    display = null,
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testBankAccountLiveModeEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.BankAccount(
                    id = "wAAACGA",
                    last4 = "6789",
                    bankAccountName = "Chase Checking Account",
                    bankIconCode = "morganchase",
                    isDefault = false,
                    nickname = null,
                    billingAddress = null,
                    billingEmailAddress = null,
                    display = null,
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testPassThroughEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Passthrough(
                    id = "csmrpd_wAAACGA",
                    last4 = "6789",
                    paymentMethodId = "pm_123",
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testUnknownWithSublabelEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Generic(
                    id = "csmrpd_126",
                    last4 = "0x••••22Dd",
                    isDefault = false,
                    nickname = null,
                    billingAddress = null,
                    billingEmailAddress = null,
                    rawType = "CRYPTO",
                    display = ConsumerPaymentDetails.Display(
                        label = "Crypto",
                        sublabel = "0x••••22Dd",
                        icon = null
                    ),
                    nextActionTypes = emptyList()
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testUnknownWithoutSublabelEnabled() {
        snapshot(
            state = State(
                details = ConsumerPaymentDetails.Generic(
                    id = "csmrpd_126",
                    last4 = "0x••••22Dd",
                    isDefault = false,
                    nickname = null,
                    billingAddress = null,
                    billingEmailAddress = null,
                    rawType = "CRYPTO",
                    display = ConsumerPaymentDetails.Display(
                        label = "Crypto",
                        sublabel = null,
                        icon = null
                    ),
                    nextActionTypes = emptyList()
                ),
                enabled = true,
                isSelected = false,
                isAvailable = true,
                isUpdating = false
            )
        )
    }

    @Test
    fun testRemoteIcons() {
        val cardIconUrl = "https://b.stripecdn.com/card-icon.png"
        val bankIconUrl = "https://b.stripecdn.com/bank-icon.png"

        // Given a card and bank account with loadable icons, and a card whose icon can't be loaded
        val paymentDetails = listOf(
            remoteIconCard(
                id = "1",
                isDefault = true,
                brand = CardBrand.Visa,
                last4 = "4242",
                label = "Visa Credit",
                iconUrl = cardIconUrl,
            ),
            ConsumerPaymentDetails.BankAccount(
                id = "2",
                last4 = "6789",
                isDefault = false,
                nickname = null,
                bankAccountName = "Checking",
                bankIconCode = null,
                billingAddress = null,
                billingEmailAddress = null,
                display = remoteIconDisplay(label = "Checking", sublabel = "••6789", iconUrl = bankIconUrl),
            ),
            remoteIconCard(
                id = "3",
                isDefault = false,
                brand = CardBrand.AmericanExpress,
                last4 = "0005",
                label = "American Express Credit",
                iconUrl = "https://b.stripecdn.com/unloadable-icon.png",
            ),
        )

        // ...and an image loader with card and bank account icons
        val images = {
            mapOf(
                cardIconUrl to solidBitmap(color = Color.RED, width = 48, height = 32),
                bankIconUrl to solidBitmap(color = Color.BLUE, width = 96, height = 96),
            )
        }

        // When each payment method is displayed
        // Then the card icon fills the rectangular icon area, the bank account icon fills the square icon area,
        // and the card without a loadable icon shows its local brand icon
        snapshotWithImageLoader(paymentDetails = paymentDetails, images = images)
    }

    private fun snapshot(state: State) {
        paparazziRule.snapshot {
            DefaultLinkTheme {
                PaymentDetailsListItem(
                    paymentDetails = state.details,
                    isClickable = state.enabled,
                    isMenuButtonClickable = true,
                    isAvailable = state.isAvailable,
                    isSelected = state.isSelected,
                    isUpdating = state.isUpdating,
                    onClick = {},
                    onMenuButtonClick = {}
                )
            }
        }
    }

    /**
     * Snapshots [paymentDetails] after their icons load from [images], which is invoked during rendering because
     * bitmaps can only be created once Paparazzi is rendering.
     */
    private fun snapshotWithImageLoader(
        paymentDetails: List<ConsumerPaymentDetails.PaymentDetails>,
        images: () -> Map<String, Bitmap>,
    ) {
        paparazziRule.snapshotAfterEffects {
            val imageLoader = remember { FakeStripeImageLoader(images = images()) }

            DefaultLinkTheme {
                CompositionLocalProvider(LocalStripeImageLoader provides imageLoader) {
                    Column {
                        paymentDetails.forEach { details ->
                            PaymentDetailsListItem(
                                paymentDetails = details,
                                isClickable = true,
                                isMenuButtonClickable = true,
                                isAvailable = true,
                                isSelected = false,
                                isUpdating = false,
                                onClick = {},
                                onMenuButtonClick = {}
                            )
                        }
                    }
                }
            }
        }
    }

    private fun remoteIconCard(
        id: String,
        isDefault: Boolean,
        brand: CardBrand,
        last4: String,
        label: String,
        iconUrl: String,
    ) = ConsumerPaymentDetails.Card(
        id = id,
        expiryYear = 2030,
        expiryMonth = 10,
        isDefault = isDefault,
        brand = brand,
        last4 = last4,
        cvcCheck = CvcCheck.Pass,
        networks = emptyList(),
        funding = ConsumerPaymentDetails.Card.Funding.Credit,
        nickname = null,
        display = remoteIconDisplay(label = label, sublabel = "•••• $last4", iconUrl = iconUrl),
    )

    private fun remoteIconDisplay(label: String, sublabel: String, iconUrl: String) = ConsumerPaymentDetails.Display(
        label = label,
        sublabel = sublabel,
        icon = ConsumerPaymentDetails.Display.Icon(defaultUrl = iconUrl),
    )

    private fun solidBitmap(color: Int, width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
    }

    internal data class State(
        val details: ConsumerPaymentDetails.PaymentDetails,
        val enabled: Boolean,
        val isSelected: Boolean,
        val isAvailable: Boolean,
        val isUpdating: Boolean,
    )
}
