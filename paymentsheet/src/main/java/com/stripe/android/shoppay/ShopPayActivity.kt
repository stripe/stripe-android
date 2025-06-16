package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf

internal class ShopPayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let { args ->
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
        Button(
            onClick = {
                dismissWithResult(
                    ShopPayActivityResult.Completed
                )
            }
        ) {
            Text("Complete")
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
