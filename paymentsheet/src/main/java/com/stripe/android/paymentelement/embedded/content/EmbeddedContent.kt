package com.stripe.android.paymentelement.embedded.content

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuth
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineTypingPadding
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationCard
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkVerificationDialogHost
import com.stripe.android.paymentelement.embedded.linkinline.rememberEmbeddedLinkInlineTyping
import com.stripe.android.paymentelement.embedded.linkinline.rememberEmbeddedVisibilityModifier
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodEmbeddedLayoutUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.utils.collectAsState
import java.io.Closeable

@Immutable
internal data class EmbeddedContent(
    private val interactor: PaymentMethodVerticalLayoutInteractor,
    private val embeddedViewDisplaysMandateText: Boolean,
    private val appearance: PaymentSheet.Appearance,
    private val isImmediateAction: Boolean,
    private val linkInlineAuth: EmbeddedLinkInlineAuth,
) : Closeable {
    override fun close() {
        interactor.close()
    }

    @Composable
    fun Content() {
        val embeddedAppearance = appearance.embeddedAppearance

        /**
         * This validation is here because of a weird interaction with 2-Step Integration.
         *
         * If this were in configure or when state is set, then it would fail for the 1st instance of embedded
         * in the 2 step integration because a user would not set a rowSelectionBehavior on the 1st instance of embedded
         * because the 1st instance doesn't show a UI. However, the first instance still has to be configured so it will
         * fail unless the user sets an empty rowSelection ImmediateAction callback.
         *
         * Having validation here ensures that we only validate when the embedded content is shown.
         */
        LaunchedEffect(embeddedAppearance.style, isImmediateAction) {
            if (embeddedAppearance.style is Embedded.RowStyle.FlatWithDisclosure && !isImmediateAction) {
                throw IllegalArgumentException(
                    "EmbeddedPaymentElement.Builder.rowSelectionBehavior() must be set to ImmediateAction when using " +
                        "FlatWithDisclosure RowStyle. Use a different style or enable ImmediateAction " +
                        "rowSelectionBehavior"
                )
            }
        }

        PaymentElementTheme(appearance = appearance) {
            val linkInlineCard by linkInlineAuth.card.collectAsState()
            val isTyping by linkInlineAuth.isTyping.collectAsState()
            val typing = rememberEmbeddedLinkInlineTyping(linkInlineAuth)
            // The containers stay in place when configure replaces the list.
            Column(modifier = typing.containerModifier) {
                Column(
                    modifier = Modifier
                        .animateContentSize()
                        .then(rememberEmbeddedVisibilityModifier(linkInlineAuth))
                ) {
                    PaymentMethodEmbeddedLayoutUI(
                        interactor = interactor,
                        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                        linkInlineCard = linkInlineCard?.let { card ->
                            {
                                EmbeddedLinkInlineVerificationCard(
                                    state = card.state,
                                    otpElement = card.otpElement,
                                    linkBrand = card.linkBrand,
                                    rowStyle = embeddedAppearance.style,
                                    isTyping = isTyping,
                                    focusRegionModifier = typing.focusRegionModifier,
                                    onCodeTapped = linkInlineAuth::onCodeTapped,
                                    onDone = linkInlineAuth::endTyping,
                                    onTypingInterrupted = linkInlineAuth::endTyping,
                                    onClose = linkInlineAuth::onClose,
                                    onSwitchAccount = linkInlineAuth::onSwitchAccount,
                                    onAction = linkInlineAuth::onAction,
                                )
                            }
                        },
                        appearance = embeddedAppearance
                    )
                }
                // Outside the animated container, so the room is there as soon as the card is scrolled.
                EmbeddedLinkInlineTypingPadding(typing)
            }
            EmbeddedLinkVerificationDialogHost(linkInlineAuth)
        }
    }
}
