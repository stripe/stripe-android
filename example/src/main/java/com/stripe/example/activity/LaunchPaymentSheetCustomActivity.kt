package com.stripe.example.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.stripe.example.databinding.ActivityPaymentSheetCustomBinding
import com.stripe.example.paymentsheet.EphemeralKey
import com.stripe.example.paymentsheet.PaymentSheetViewModel

class LaunchPaymentSheetCustomActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityPaymentSheetCustomBinding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            PaymentSheetViewModel.Factory(
                application,
                getPreferences(Context.MODE_PRIVATE)
            )
        )[PaymentSheetViewModel::class.java]
    }

    private lateinit var ephemeralKey: EphemeralKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) {
            viewBinding.progressBar.visibility = if (it) View.VISIBLE else View.INVISIBLE
            viewBinding.launch.isEnabled = !it
            viewBinding.clear.isEnabled = !it
        }
        viewModel.status.observe(this) {
            viewBinding.status.text = it
        }

        viewBinding.clear.setOnClickListener {
            viewModel.clearKeys()
            fetchEphemeralKey()
        }
        viewBinding.launch.setOnClickListener {
            // TODO(mshafrir-stripe): handle click
        }
        fetchEphemeralKey()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // handle result
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
