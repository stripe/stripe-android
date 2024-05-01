package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.utils.stateFlowOf

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CvcRecollectionField(element: CvcElement, cardBrand: CardBrand, lastFour: String) {
    val backgroundColor = if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.075f)
    } else {
        Color.Black.copy(alpha = 0.075f)
    }

    return SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .weight(.5f, true)
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = cardBrand.icon),
                        contentDescription = "",
                    )
                    Text(
                        text = stringResource(
                            R.string.stripe_paymentsheet_payment_method_item_card_number,
                            lastFour
                        ),
                        style = MaterialTheme.typography.body1
                    )
                }

            }
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.stripeColors.componentDivider
            )
            element.controller.ComposeUI(
                enabled = true,
                field = element,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(.5f, true),
                hiddenIdentifiers = setOf(),
                lastTextFieldIdentifier = null,
                nextFocusDirection = FocusDirection.Exit,
                previousFocusDirection = FocusDirection.Previous
            )
        }
    }
}

@Composable
@Preview
private fun CvcRecollectionFieldPreview() {
    val element = remember {
        CvcElement(IdentifierSpec(), CvcController(cardBrandFlow = stateFlowOf(CardBrand.Unknown)))
    }

    StripeTheme {
        Column(
            Modifier
                .background(Color.White)
                .padding(20.dp)
        ) {
            CvcRecollectionField(element = element, cardBrand = CardBrand.Visa, lastFour = "4242")
        }
    }
}
