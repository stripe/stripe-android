package com.stripe.android.paymentsheet.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.networking.StripeNetworkClientInterceptor
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.paymentsheet.example.databinding.ActivityMainBinding
import com.stripe.android.paymentsheet.example.devtools.DevToolsActivityLifecycleCallbacks
import com.stripe.android.paymentsheet.example.devtools.DevToolsStore
import com.stripe.android.paymentsheet.example.devtools.addDevToolsMenu
import com.stripe.android.paymentsheet.example.playground.activity.AppearancePlaygroundActivity
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.samples.activity.LaunchPaymentSheetCompleteActivity
import com.stripe.android.paymentsheet.example.samples.activity.LaunchPaymentSheetCustomActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbar)

        addDevToolsMenu()
        application.registerActivityLifecycleCallbacks(DevToolsActivityLifecycleCallbacks())

        viewBinding.launchCompleteButton.setOnClickListener {
            startActivity(Intent(this, LaunchPaymentSheetCompleteActivity::class.java))
        }

        viewBinding.launchCustomButton.setOnClickListener {
            startActivity(Intent(this, LaunchPaymentSheetCustomActivity::class.java))
        }

        viewBinding.launchPlaygroundButton.setOnClickListener {
            startActivity(Intent(this, PaymentSheetPlaygroundActivity::class.java))
        }

        viewBinding.appearanceButton.setOnClickListener {
            startActivity(Intent(this, AppearancePlaygroundActivity::class.java))
        }

        viewBinding.version.text = StripeSdkVersion.VERSION_NAME

        StripeNetworkClientInterceptor.setFailureEvaluator { requestUrl ->
            DevToolsStore.shouldFailFor(requestUrl)
        }

        lifecycleScope.launch {
            DevToolsStore.loadEndpoints()
        }
    }
}
