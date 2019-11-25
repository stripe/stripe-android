package com.stripe.example.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.example.R
import com.stripe.example.module.DependencyHandler

class CreateCardTokenActivity : AppCompatActivity() {

    private lateinit var dependencyHandler: DependencyHandler

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.card_token_activity)

        dependencyHandler = DependencyHandler(
            this,
            findViewById(R.id.card_input_widget),
            findViewById(R.id.tokens_list),
            PaymentConfiguration.getInstance(this).publishableKey
        )

        dependencyHandler.attachAsyncTaskTokenController(findViewById(R.id.create_token_asynctask))
        dependencyHandler.attachRxTokenController(findViewById(R.id.create_token_rxjava))
    }

    override fun onDestroy() {
        dependencyHandler.clearReferences()
        super.onDestroy()
    }
}
