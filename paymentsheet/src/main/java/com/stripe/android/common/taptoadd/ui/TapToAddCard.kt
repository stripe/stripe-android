package com.stripe.android.common.taptoadd.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.getDayIcon

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun TapToAddCard(
    cardBrand: CardBrand,
    last4: String?,
) {
    val sharedElementScope = LocalSharedElementScope.current

    sharedElementScope?.let {
        with(sharedElementScope.sharedTransitionScope) {
            Card(
                cardBrand = cardBrand,
                last4 = last4,
                modifier = Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(SHARED_CARD_ELEMENT_KEY),
                    animatedVisibilityScope = sharedElementScope.animatedVisibilityScope,
                )
            )
        }
    } ?: run {
        Card(
            cardBrand = cardBrand,
            last4 = last4,
        )
    }
}

@Composable
private fun Card(
    cardBrand: CardBrand,
    last4: String?,
    modifier: Modifier = Modifier,
) {
    val knownCardArt = when (cardBrand) {
        CardBrand.Visa -> R.drawable.stripe_tta_card_art_visa
        CardBrand.JCB -> R.drawable.stripe_tta_card_art_jcb
        CardBrand.Discover -> R.drawable.stripe_tta_card_art_discover
        else -> null
    }

    val textColor = when (cardBrand) {
        CardBrand.Visa -> Color.White
        CardBrand.JCB -> Color.White
        CardBrand.Discover -> Color.Black
        else -> null
    }

    val textPaddingModifier = when (cardBrand) {
        CardBrand.Discover,
        CardBrand.Visa -> Modifier.padding(
            horizontal = 20.dp,
            vertical = 10.dp,
        )
        else -> Modifier.padding(
            horizontal = 20.dp,
            vertical = 30.dp,
        )
    }

    Box(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Max)
    ) {
        Image(
            painter = painterResource(knownCardArt ?: R.drawable.stripe_tta_card_background),
            contentDescription = null,
        )

        last4?.let {
            Text(
                text = "···· $last4",
                color = textColor ?: Color.Black,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = FontFamily.Monospace,
                ),
                modifier = textPaddingModifier.align(Alignment.BottomStart),
            )
        }

        if (knownCardArt == null) {
            Image(
                modifier = Modifier
                    .padding(
                        horizontal = 20.dp,
                        vertical = 10.dp,
                    )
                    .size(70.dp)
                    .align(Alignment.BottomEnd),
                painter = painterResource(cardBrand.getDayIcon()),
                contentDescription = null,
            )
        }
    }
}

private const val SHARED_CARD_ELEMENT_KEY = "STRIPE_TTA_CARD_LAYOUT"
