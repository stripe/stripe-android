package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.core.R as StripeCoreR

@Composable
internal fun ColumnScope.TapToAddErrorScreen(
    message: ResolvableString,
) {
    Spacer(Modifier.weight(1f))

    Image(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        painter = painterResource(R.drawable.stripe_ic_warning_symbol),
        contentDescription = null,
        colorFilter = ColorFilter.tint(
            color = MaterialTheme.colors.primaryVariant,
        ),
    )

    Spacer(Modifier.size(25.dp))

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(StripeCoreR.string.stripe_error),
        color = MaterialTheme.colors.secondaryVariant,
        style = MaterialTheme.typography.h5,
        fontSize = StripeTheme.typographyMutable.xLargeFontSize,
        fontWeight = FontWeight.W800,
    )

    Spacer(Modifier.size(15.dp))

    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = message.resolve(),
        color = MaterialTheme.colors.secondaryVariant,
        style = MaterialTheme.typography.h5,
        fontSize = StripeTheme.typographyMutable.xLargeFontSize,
        fontWeight = FontWeight.W400
    )

    Spacer(Modifier.weight(1f))
}
