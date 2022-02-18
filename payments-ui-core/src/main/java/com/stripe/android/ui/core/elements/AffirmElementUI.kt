package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.stripe.android.ui.core.R

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AffirmElementUI(
//    element: AffirmHeaderElement
) {
//    val context = LocalContext.current

    FlowRow(
        modifier = Modifier.padding(4.dp, 8.dp, 4.dp, 4.dp),
        crossAxisAlignment = FlowCrossAxisAlignment.Center
    ) {
        Text(
//            element.getLabel(context.resources),
            stringResource(R.string.affirm_buy_now_pay_later),
            Modifier
                .padding(end = 4.dp)
                .padding(top = 6.dp),
            color = if (isSystemInDarkTheme()) {
                Color.LightGray
            } else {
                Color.Black
            }
        )
        Image(
            painter = if (isSystemInDarkTheme()) {
                painterResource(R.drawable.stripe_ic_affirm_logo_dark)
            } else {
                painterResource(R.drawable.stripe_ic_affirm_logo_light)
            },
            contentDescription = stringResource(
                R.string.affirm_buy_now_pay_later
            )
        )
    }
}
