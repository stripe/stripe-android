package com.stripe.example.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.example.databinding.ActivityPaymentSheetCustomBinding
import com.stripe.example.paymentsheet.EphemeralKey
import com.stripe.example.paymentsheet.PaymentSheetViewModel

class LaunchPaymentSheetCustomActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCustomBinding.inflate(layoutInflater)
    }

    private val viewModel: PaymentSheetViewModel by viewModels {
        PaymentSheetViewModel.Factory(
            application,
            getPreferences(Context.MODE_PRIVATE)
        )
    }

    private lateinit var ephemeralKey: EphemeralKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            viewBinding.buyButton.isEnabled = !it
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.buyButton.setOnClickListener {
            // TODO(mshafrir-stripe): handle click
        }
        fetchEphemeralKey()
    }

    private fun fetchEphemeralKey() {
        viewModel.fetchEphemeralKey()
            .observe(this) { newEphemeralKey ->
                if (newEphemeralKey != null) {
                    ephemeralKey = newEphemeralKey
                }
            }
    }
}
