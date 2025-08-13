package com.stripe.android.ui.core.elements

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cardscan.CardScanContract
import com.stripe.android.uicore.elements.H6Text
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI
import com.stripe.android.uicore.utils.AnimationConstants

@Suppress("LongMethod")
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CardDetailsSectionElementUI(
    enabled: Boolean,
    controller: CardDetailsSectionController,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier,
) {
    if (
        controller.isCardScanEnabled &&
        controller.isStripeCardScanAvailable() &&
        controller.autoCardScanData?.shouldOpenCardScanAutomatically == true
    ) {
        val cardScanLauncher =
            rememberLauncherForActivityResult(CardScanContract()) { result ->
                controller.onCardScanResult(result)
            }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            LocalContext.current,
            AnimationConstants.FADE_IN,
            AnimationConstants.FADE_OUT,
        )

        SideEffect {
            controller.autoCardScanData.hasSeenAutoCardScan = true

            cardScanLauncher.launch(
                input = CardScanContract.Args(
                    configuration = CardScanConfiguration(
                        elementsSessionId = controller.elementsSessionId
                    )
                ),
                options
            )
        }
    }
    Column(modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            H6Text(
                text = stringResource(R.string.stripe_paymentsheet_add_payment_method_card_information),
                modifier = Modifier
                    .semantics(mergeDescendants = true) { // Need to prevent form as focusable accessibility
                        heading()
                    }
            )
            if (controller.isCardScanEnabled && controller.isStripeCardScanAvailable()) {
                ScanCardButtonUI(
                    enabled = enabled,
                    elementsSessionId = controller.elementsSessionId
                ) {
                    controller.onCardScanResult(it)
                }
            }
        }
        SectionElementUI(
            modifier = Modifier.padding(top = 8.dp),
            enabled = enabled,
            element = SectionElement(
                IdentifierSpec.Generic("credit_details"),
                listOf(controller.cardDetailsElement),
                SectionController(
                    null,
                    listOf(controller.cardDetailsElement.sectionFieldErrorController())
                )
            ),
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier
        )
    }
}
