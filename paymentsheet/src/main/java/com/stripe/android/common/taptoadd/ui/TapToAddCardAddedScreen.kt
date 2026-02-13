package com.stripe.android.common.taptoadd.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun TapToAddCardAddedScreen(
    cardBrand: CardBrand,
    last4: String?,
    onComplete: () -> Unit,
) {
    val onCompleteRef = remember { mutableStateOf(onComplete) }

    onCompleteRef.value = onComplete

    val cardVisible = remember { MutableTransitionState(false) }
    val titleVisible = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        delay(1.seconds)
        cardVisible.targetState = true
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            cardVisible.isIdle && cardVisible.currentState
        }.collect { isComplete ->
            if (isComplete) {
                delay(1.seconds)
                titleVisible.targetState = true
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            titleVisible.isIdle && titleVisible.currentState
        }.collect { isComplete ->
            if (isComplete) {
                delay(1.seconds)
                onCompleteRef.value()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = maxHeight * 0.1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visibleState = cardVisible,
                enter = fadeIn(),
            ) {
                TapToAddCardLayout(cardBrand, last4)
            }

            Spacer(Modifier.size(20.dp))

            AnimatedVisibility(
                visibleState = titleVisible,
                enter = fadeIn(),
            ) {
                Title()
            }
        }
    }
}

@Composable
private fun Title() {
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.stripe_tap_to_add_card_added_title))
            append(" ")
            appendInlineContent(CHECKMARK_ID)
        },
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h4.copy(
            fontWeight = FontWeight.Normal,
        ),
        inlineContent = mapOf(
            CHECKMARK_ID to InlineTextContent(
                placeholder = Placeholder(
                    width = MaterialTheme.typography.h5.fontSize,
                    height = MaterialTheme.typography.h5.fontSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                Icon(
                    painter = painterResource(com.stripe.android.uicore.R.drawable.stripe_ic_checkmark),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface
                )
            }
        ),
    )
}

private const val CHECKMARK_ID = "STRIPE_TTA_CARD_ADDED_CHECKMARK"
