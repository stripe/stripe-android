package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState

/**
 * Activity that handles Shop Pay authentication via WebView.
 */
internal class ShopPayActivity : ComponentActivity() {
    private var args: ShopPayArgs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args = intent.extras?.let { args ->
            BundleCompat.getParcelable(args, EXTRA_ARGS, ShopPayArgs::class.java)
        }

        if (args == null) {
            dismissWithResult(ShopPayActivityResult.Failed(Throwable("No args")))
            return
        }

        setContent {
            Content()
        }
    }

    @Composable
    private fun Content() {
        val bottomSheetState = rememberStripeBottomSheetState()

        ElementsBottomSheetLayout(
            state = bottomSheetState,
            onDismissed = {
                dismissWithResult(ShopPayActivityResult.Canceled)
            }
        ) {
            BottomSheetScaffold(
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        IconButton(
                            modifier = Modifier.align(Alignment.CenterStart),
                            onClick = {
                                dismissWithResult(ShopPayActivityResult.Canceled)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                                contentDescription = "Close"
                            )
                        }
                    }
                },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Button(
                            onClick = {
                                dismissWithResult(ShopPayActivityResult.Completed("pm_1234"))
                            }
                        ) {
                            Text("Complete")
                        }

                        Button(
                            onClick = {
                                dismissWithResult(ShopPayActivityResult.Failed(Throwable("Failed")))
                            }
                        ) {
                            Text("Fail")
                        }
                    }
                }
            )
        }
    }

    private fun dismissWithResult(result: ShopPayActivityResult) {
        val bundle = bundleOf(
            ShopPayActivityContract.EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
    }

    companion object {
        internal const val EXTRA_ARGS = "shop_pay_args"
        internal const val RESULT_COMPLETE = 63636

        internal fun createIntent(
            context: Context,
            args: ShopPayArgs
        ): Intent {
            return Intent(context, ShopPayActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }
    }
}
