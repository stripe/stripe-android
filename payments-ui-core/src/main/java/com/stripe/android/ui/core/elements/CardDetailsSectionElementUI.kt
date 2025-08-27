package com.stripe.android.ui.core.elements

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
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cardscan.CardScanGoogleLauncher.Companion.rememberCardScanGoogleLauncher
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.uicore.elements.H6Text
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionElementUI

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CardDetailsSectionElementUI(
    enabled: Boolean,
    controller: CardDetailsSectionController,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier,
) {
    if (controller.shouldAutomaticallyLaunchCardScan()) {
        val context = LocalContext.current
        val eventsReporter = LocalCardScanEventsReporter.current
        val cardScanGoogleLauncher = rememberCardScanGoogleLauncher(
            context,
            eventsReporter,
            controller.cardDetailsElement.controller.onCardScanResult
        )

        SideEffect {
            controller.setHasAutomaticallyLaunchedCardScan()

            cardScanGoogleLauncher.launch(context)
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
            ScanCardButtonUI(
                enabled = enabled,
                controller = controller
            )
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
