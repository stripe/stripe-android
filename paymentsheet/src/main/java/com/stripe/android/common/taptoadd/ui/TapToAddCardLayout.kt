package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.getDayIcon

@Composable
internal fun TapToAddCardLayout(
    cardBrand: CardBrand,
    last4: String?,
) {
    Box(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .height(IntrinsicSize.Max)
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_tta_card_background),
            contentDescription = null,
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,

        ) {
            last4?.let {
                Text(
                    text = "···· $last4",
                    color = MaterialTheme.colors.surface,
                    style = MaterialTheme.typography.h4.copy(
                        fontWeight = FontWeight.Normal
                    ),
                )
            }

            Spacer(Modifier.weight(1f))

            Image(
                modifier = Modifier.size(70.dp),
                painter = painterResource(cardBrand.getDayIcon()),
                contentDescription = null,
            )
        }
    }
}
