package com.stripe.android.paymentelement.embedded.linkinline

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.Action
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.Actions
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineCardState.MenuItem
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.R
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class EmbeddedLinkInlineVerificationCardScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        boxModifier = Modifier.padding(16.dp),
    )

    @Test
    fun testFlatWithRadio() {
        paparazziRule.snapshot {
            TestCard(rowStyle = RowStyle.FlatWithRadio.default)
        }
    }

    @Test
    fun testFloatingButton() {
        paparazziRule.snapshot {
            TestCard(rowStyle = RowStyle.FloatingButton.default)
        }
    }

    @Test
    fun testFlatWithCheckmark() {
        paparazziRule.snapshot {
            TestCard(rowStyle = RowStyle.FlatWithCheckmark.default)
        }
    }

    @Test
    fun testFlatWithDisclosure() {
        paparazziRule.snapshot {
            TestCard(rowStyle = RowStyle.FlatWithDisclosure.default)
        }
    }

    @Test
    fun testSending() {
        paparazziRule.snapshot {
            TestCard(
                state = CARD_STATE.copy(
                    isCodeEntryEnabled = false,
                    actions = MENU.copy(isEnabled = false),
                    isActionLoading = true,
                ),
            )
        }
    }

    @Test
    fun testError() {
        paparazziRule.snapshot {
            TestCard(
                state = CARD_STATE.copy(errorMessage = "That code is invalid. Try again.".resolvableString),
            )
        }
    }

    @Test
    fun testResendOnly() {
        paparazziRule.snapshot {
            TestCard(
                state = CARD_STATE.copy(
                    actions = Actions.Resend(
                        label = resolvableString(R.string.stripe_verification_resend),
                        isEnabled = true,
                    ),
                ),
            )
        }
    }

    @Test
    fun testResendCountdown() {
        paparazziRule.snapshot {
            TestCard(
                state = CARD_STATE.copy(
                    actions = Actions.Resend(
                        label = resolvableString(R.string.stripe_link_auth_resend_countdown, 7),
                        isEnabled = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun testLongEmail() {
        paparazziRule.snapshot {
            TestCard(
                state = CARD_STATE.copy(email = "jenny.rosen.with.a.very.long.email.address@example.com"),
            )
        }
    }

    @Test
    fun testOnelink() {
        paparazziRule.snapshot {
            TestCard(linkBrand = LinkBrand.Onelink)
        }
    }

    @Composable
    private fun TestCard(
        state: EmbeddedLinkInlineCardState = CARD_STATE,
        rowStyle: RowStyle = RowStyle.FlatWithRadio.default,
        linkBrand: LinkBrand = LinkBrand.Link,
    ) {
        EmbeddedLinkInlineVerificationCard(
            state = state,
            otpElement = OTPElementFactory.create(),
            linkBrand = linkBrand,
            rowStyle = rowStyle,
            onCodeTapped = {},
            onClose = {},
            onSwitchAccount = {},
            onAction = {},
        )
    }

    internal companion object {
        val MENU = Actions.Menu(
            items = listOf(
                MenuItem(
                    action = Action.Resend,
                    label = resolvableString(R.string.stripe_verification_resend),
                    isEnabled = true,
                ),
                MenuItem(
                    action = Action.Email,
                    label = resolvableString(R.string.stripe_link_auth_email_code),
                    isEnabled = true,
                ),
            ),
            isEnabled = true,
        )

        val CARD_STATE = EmbeddedLinkInlineCardState(
            email = "jenny.rosen@example.com",
            recipient = "(•••) ••• ••55",
            isCodeEntryEnabled = true,
            errorMessage = null,
            actions = MENU,
            isActionLoading = false,
            inputRevision = 0,
            invalidCodeRevision = 0,
        )
    }
}
