import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

class FawryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaymentSheetExampleTheme {
                Column {
                    ResultButton(result = ExternalPaymentMethodResult.Completed)
                    ResultButton(result = ExternalPaymentMethodResult.Canceled)
                    ResultButton(result = ExternalPaymentMethodResult.Failed)
                }
            }
        }
    }

    fun finishWithResult(result : ExternalPaymentMethodResult) {
        val resultCode = result.resultCode
        var data: Intent? = null
        if (result is ExternalPaymentMethodResult.Failed) {
            data = Intent().putExtra(ExternalPaymentMethodResult.Failed.ERROR_MESSAGE_EXTRA, "Payment failed!")
        }
        setResult(resultCode, data)
        finish()
    }

    @Composable
    fun ResultButton(result : ExternalPaymentMethodResult) {
        Button(onClick = { finishWithResult(result) }) {
            Text(text = result.toString())
        }
    }

    class FawryConfirmHandler : ExternalPaymentMethodConfirmHandler {
        override fun createIntent(
            context: Context,
            externalPaymentMethodType: String, billingDetails: PaymentSheet.BillingDetails
        ): Intent {
            return Intent().setClass(
                context, FawryActivity::class.java
            ).putExtra("epm_type", externalPaymentMethodType).putExtra("billing_details", billingDetails)
        }
    }
}
