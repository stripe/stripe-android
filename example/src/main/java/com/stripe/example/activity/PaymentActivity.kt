package com.stripe.example.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button

import com.stripe.android.PaymentConfiguration
import com.stripe.example.R
import com.stripe.example.module.DependencyHandler

class PaymentActivity : AppCompatActivity() {

    private lateinit var dependencyHandler: DependencyHandler

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.payment_activity)

        dependencyHandler = DependencyHandler(
            this,
            findViewById(R.id.card_input_widget),
            findViewById(R.id.listview),
            PaymentConfiguration.getInstance().publishableKey
        )

        val saveButton = findViewById<Button>(R.id.save)
        dependencyHandler.attachAsyncTaskTokenController(saveButton)

        val saveRxButton = findViewById<Button>(R.id.saverx)
        dependencyHandler.attachRxTokenController(saveRxButton)

        val saveIntentServiceButton = findViewById<Button>(R.id.saveWithService)
        dependencyHandler.attachIntentServiceTokenController(this,
            saveIntentServiceButton)
    }

    override fun onDestroy() {
        dependencyHandler.clearReferences()
        super.onDestroy()
    }
}
