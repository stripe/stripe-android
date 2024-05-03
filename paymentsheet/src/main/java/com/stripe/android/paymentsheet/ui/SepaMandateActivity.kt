package com.stripe.android.paymentsheet.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.ui.core.R as StripeUiCoreR

@OptIn(ExperimentalMaterialApi::class)
internal class SepaMandateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val merchantName = runCatching {
            requireNotNull(SepaMandateContract.Args.fromIntent(intent)) {
                "SepaMandateActivity was started without arguments."
            }
        }.getOrNull()?.merchantName

        if (merchantName == null) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            StripeTheme {
                val bottomSheetState = rememberBottomSheetState()
                BottomSheet(state = bottomSheetState, onDismissed = { finish() }) {
                    SepaMandateScreen(
                        merchantName = merchantName,
                        acknowledgedCallback = {
                            val result = Intent().putExtra(
                                SepaMandateContract.EXTRA_RESULT,
                                SepaMandateResult.Acknowledged,
                            )
                            setResult(RESULT_OK, result)
                            finish()
                        },
                        closeCallback = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun SepaMandateScreen(
    merchantName: String,
    acknowledgedCallback: () -> Unit,
    closeCallback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
    ) {
        IconButton(
            onClick = closeCallback,
            modifier = Modifier
                .testTag("SEPA_MANDATE_CLOSE_BUTTON")
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                contentDescription = stringResource(R.string.stripe_paymentsheet_close),
                tint = MaterialTheme.stripeColors.appBarIcon,
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            H4Text(
                text = stringResource(id = StripeUiCoreR.string.stripe_paymentsheet_payment_method_sepa_debit),
            )
            Text(
                text = stringResource(
                    id = StripeUiCoreR.string.stripe_sepa_mandate,
                    merchantName
                ),
                color = MaterialTheme.stripeColors.subtitle,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Button(
                onClick = acknowledgedCallback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("SEPA_MANDATE_CONTINUE_BUTTON"),
            ) {
                Text(
                    text = stringResource(
                        id = StripeUiCoreR.string.stripe_continue_button_label
                    ),
                )
            }
        }
    }
}
