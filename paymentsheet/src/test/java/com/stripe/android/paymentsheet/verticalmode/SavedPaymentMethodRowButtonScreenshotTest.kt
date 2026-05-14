package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import kotlin.test.Test

internal class SavedPaymentMethodRowButtonScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    private val savedVisa = DisplayableSavedPaymentMethod.create(
        displayName = "···· 4242".resolvableString,
        paymentMethod = PaymentMethod(
            id = "001",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.Card.code,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = "4242",
            )
        ),
    )

    @Test
    fun testSavedVisa() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedVisa_selected() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = true,
            )
        }
    }

    @Test
    fun testSavedVisa_disabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                linkBrand = LinkBrand.Link,
                isEnabled = false,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedVisa_default() {
        val savedDefaultVisa = PaymentMethodFixtures.defaultDisplayableCard()

        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedDefaultVisa,
                linkBrand = LinkBrand.Link,
                isEnabled = false,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedLinkPaymentMethod() {
        val savedDefaultVisa = PaymentMethodFixtures.displayableLinkPaymentMethod()

        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedDefaultVisa,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedLinkCardBrandPaymentMethod() {
        val savedDefaultVisa = PaymentMethodFixtures.displayableLinkCardBrandPaymentMethod()

        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedDefaultVisa,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedVisa_withCardArt() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisaWithCardArt,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedVisa_withCardArt_selected() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisaWithCardArt,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = true,
            )
        }
    }

    @Test
    fun testSavedLinkPassthroughCard() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedLinkPassthroughCard,
                linkBrand = LinkBrand.Link,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedNotlinkPassthroughCard() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedLinkPassthroughCard,
                linkBrand = LinkBrand.Notlink,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    private companion object {
        const val SAMPLE_CARD_ART_URL =
            "https://b.stripecdn.com/cardart/assets/pfE0FkDGaiFhdoOj9to8po-ZLiJhetgfdKELIZCj3xA"

        val savedLinkPassthroughCard = DisplayableSavedPaymentMethod.create(
            displayName = "4242".resolvableString,
            paymentMethod = PaymentMethod(
                id = "001",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.Card.code,
                type = PaymentMethod.Type.Card,
                isLinkPassthroughMode = true,
                card = PaymentMethod.Card(
                    brand = CardBrand.Visa,
                    last4 = "4242",
                )
            ),
        )

        val savedVisaWithCardArt = DisplayableSavedPaymentMethod.create(
            displayName = "···· 4242".resolvableString,
            paymentMethod = PaymentMethod(
                id = "001",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.Card.code,
                type = PaymentMethod.Type.Card,
                card = PaymentMethod.Card(
                    brand = CardBrand.Visa,
                    last4 = "4242",
                    cardArt = PaymentMethod.Card.CardArt(
                        artImage = PaymentMethod.Card.CardArt.ArtImage(
                            format = "image/png",
                            url = SAMPLE_CARD_ART_URL
                        ),
                        programName = "Test Program"
                    )
                )
            ),
            )
    }
}
