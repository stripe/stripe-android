package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cardscan.CardScanActivity

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CardDetailsSectionElementUI(
    enabled: Boolean,
    controller: CardDetailsSectionController,
    hiddenIdentifiers: List<IdentifierSpec>?
) {
    val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        H6Text(
            text = stringResource(R.string.card_information),
            modifier = Modifier
                .semantics(mergeDescendants = true) { // Need to prevent form as focusable accessibility
                    heading()
                }
        )
        if (isStripeCardScanAvailable()) {
            ScanCardButtonUI {
                controller.cardDetailsElement.controller.numberElement.controller.onCardScanResult(
                    it.getParcelableExtra(CardScanActivity.CARD_SCAN_PARCELABLE_NAME)
                        ?: CardScanSheetResult.Failed(
                            UnknownScanException("No data in the result intent")
                        )
                )
            }
        }
    }
    SectionElementUI(
        enabled,
        SectionElement(
            IdentifierSpec.Generic("credit_details"),
            listOf(controller.cardDetailsElement),
            SectionController(null, listOf(controller.cardDetailsElement.sectionFieldErrorController()))
        ),
        hiddenIdentifiers ?: emptyList(),
        IdentifierSpec.Generic("card_details")
    )
}
