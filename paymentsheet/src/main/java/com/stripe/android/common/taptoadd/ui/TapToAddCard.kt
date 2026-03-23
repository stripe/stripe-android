package com.stripe.android.common.taptoadd.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.common.taptoadd.LocalTapToAddImageRepository
import com.stripe.android.common.taptoadd.TapToAddImageRepository
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R

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
        Card(cardBrand = cardBrand, last4 = last4)
    }
}

@Composable
private fun Card(
    cardBrand: CardBrand,
    last4: String?,
    modifier: Modifier = Modifier,
) {
    val imageRepository = LocalTapToAddImageRepository.current

    val state: MutableState<TapToAddImageRepository.CardArt?> = remember {
       mutableStateOf(imageRepository?.get(cardBrand))
    }

    imageRepository?.let { repository ->
        LaunchedEffect(cardBrand) {
            val image = repository.get(cardBrand)

            if (image == null) {
                state.value = repository.load(cardBrand).await()
            }
        }
    }

    AnimatedContent(
        targetState = state.value,
        label = "loading_tta_image_animation",
    ) { cardArt ->
        Box(
            modifier = modifier
                .testTag(TAP_TO_ADD_CARD_TEST_TAG)
                .width(CARD_ART_WIDTH)
                .height(CARD_ART_HEIGHT),
        ) {
            when (cardArt) {
                null -> {
                    Image(
                        painter = painterResource(R.drawable.stripe_tta_card_background),
                        contentDescription = null,
                    )

                    last4?.let {
                        CardNumber(
                            last4 = last4,
                            textColor = Color.Black,
                        )
                    }
                }
                else -> {
                    Image(
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        painter = remember {
                            BitmapPainter(cardArt.bitmap.asImageBitmap())
                        }
                    )

                    last4?.let {
                        CardNumber(
                            last4 = last4,
                            textColor = cardArt.textColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CardNumber(
    last4: String,
    textColor: Color,
) {
    Text(
        text = "···· $last4",
        color = textColor,
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.W500,
            fontFamily = FontFamily.Monospace,
        ),
        modifier = Modifier.padding(
            horizontal = 20.dp,
            vertical = 10.dp,
        ).align(Alignment.BottomStart),
    )
}

@Preview
@Composable
private fun CardArt() {
    Card(CardBrand.Visa, "4242")
}

private val CARD_ART_HEIGHT = 213.dp
private val CARD_ART_WIDTH = 341.dp

private const val SHARED_CARD_ELEMENT_KEY = "STRIPE_TTA_CARD_LAYOUT"

internal const val TAP_TO_ADD_CARD_TEST_TAG = "TAP_TO_ADD_CARD"
