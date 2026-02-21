package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
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
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.core.R as StripeCoreR

@Composable
internal fun TapToAddErrorScreen(
    message: ResolvableString,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_ic_warning_symbol),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                color = MaterialTheme.colors.primaryVariant,
            ),
        )

        Spacer(Modifier.size(50.dp))

        Text(
            text = stringResource(StripeCoreR.string.stripe_error),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.W800,
        )

        Spacer(Modifier.size(10.dp))

        Text(
            text = message.resolve(),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.W400
        )
    }
}
