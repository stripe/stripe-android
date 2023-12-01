package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stripe.android.common.ui.PrimaryButton

@Composable
internal fun BacsMandateButton(type: BacsMandateButtonType, label: String, onClick: () -> Unit) {
    when (type) {
        BacsMandateButtonType.Primary -> PrimaryButton(
            label = label,
            isEnabled = true,
            onButtonClick = onClick
        )
        BacsMandateButtonType.Secondary -> TextButton(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.primary
            ),
            onClick = onClick
        ) {
            Text(text = label)
        }
    }
}
