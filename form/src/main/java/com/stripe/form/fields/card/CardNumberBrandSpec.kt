package com.stripe.form.fields.card

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.stripe.android.model.CardBrand
import com.stripe.form.ContentSpec

@Stable
data class CardNumberBrandSpec(
    val state: State
) : ContentSpec {

    @Composable
    override fun Content(modifier: Modifier) {
        Image(
            modifier = modifier,
            painter = painterResource(state.cardBrand.icon),
            contentDescription = state.cardBrand.displayName
        )
    }

    data class State(
        val cardBrand: CardBrand
    )
}