package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

@Composable
internal fun TestResultButtons(
    finishWithResult: (TapToAddResult) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
    ) {
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethod(
                id = "card",
                created = 10,
                liveMode = false,
                code = PaymentMethod.Type.Card.code,
                type = PaymentMethod.Type.Card,
            )
        )

        Button(
            onClick = { finishWithResult(TapToAddResult.Complete) }
        ) {
            Text("Complete")
        }

        Button(
            onClick = { finishWithResult(TapToAddResult.Continue(paymentSelection)) }
        ) {
            Text("Continue")
        }

        Button(
            onClick = { finishWithResult(TapToAddResult.Canceled(null)) }
        ) {
            Text("Canceled without payment selection")
        }

        Button(
            onClick = { finishWithResult(TapToAddResult.Canceled(paymentSelection)) }
        ) {
            Text("Canceled with payment selection")
        }
    }
}
