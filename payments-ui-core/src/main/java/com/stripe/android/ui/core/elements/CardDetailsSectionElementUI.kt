package com.stripe.android.ui.core.elements

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
import com.stripe.android.ui.core.CardScanActivity
import com.stripe.android.ui.core.DefaultIsStripeCardScanAvailable
import com.stripe.android.ui.core.R

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun CardDetailsSectionElementUI(
    enabled: Boolean,
    hiddenIdentifiers: List<IdentifierSpec>?,
    context: Context
) {
    val cardDetailsElement = CardDetailsElement(
        IdentifierSpec.Generic("credit_detail"), context
    )

    val isStripeCardScanAvailable = DefaultIsStripeCardScanAvailable()

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 9.dp),
    ) {
        Text(
            text = stringResource(R.string.card_information),
            color = SectionTitle.color,
            style = MaterialTheme.typography.h6.copy(
                fontSize = SectionTitle.fontSize,
                fontWeight = SectionTitle.fontWeight,
                letterSpacing = SectionTitle.letterSpacing,
            ),
            modifier = Modifier
                .semantics(mergeDescendants = true) { // Need to prevent form as focusable accessibility
                    heading()
                }
        )
        if (isStripeCardScanAvailable()) {
            val cardScanLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.data != null) {
                        cardDetailsElement.controller.numberElement.controller.onCardScanResult(
                            it.data!!.getParcelableExtra("test")
                                ?: CardScanSheetResult.Failed(
                                    UnknownScanException("No data in the result intent")
                                )
                        )
                    }
                }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        cardScanLauncher.launch(
                            Intent(
                                context,
                                CardScanActivity::class.java
                            )
                        )
                    }
                )
            ) {
                Image(
                    painter = painterResource(R.drawable.stripe_ic_camera),
                    contentDescription = stringResource(
                        R.string.invalid_expiry_month // Update later
                    ),
                    colorFilter = if (isSystemInDarkTheme()) {
                        ColorFilter.tint(Color.White)
                    } else {
                        null
                    }
                )
                Text(
                    "Scan card",
                    Modifier
                        .padding(start = 4.dp),
                    color = if (isSystemInDarkTheme()) {
                        Color.LightGray
                    } else {
                        Color.Black
                    },
                    style = MaterialTheme.typography.h6.copy(
                        fontSize = SectionTitle.fontSize,
                        fontWeight = SectionTitle.fontWeight,
                        letterSpacing = SectionTitle.letterSpacing,
                    )
                )
            }
        }
    }
    SectionElementUI(
        enabled,
        SectionElement(
            IdentifierSpec.Generic("credit_details_section"),
            listOf(cardDetailsElement),
            SectionController(null, listOf(cardDetailsElement.sectionFieldErrorController()))
        ),
        hiddenIdentifiers ?: emptyList(),
        cardDetailsElement.controller.cvcElement.identifier
    )
}
