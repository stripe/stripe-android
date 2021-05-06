package com.stripe.example.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.view.CardFormView

abstract class BaseCFVActvity : AppCompatActivity() {
    lateinit var cardFormView: CardFormView
    lateinit var toggleButton: Button
    lateinit var getParamButton: Button
    lateinit var result: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeViews()
        cardFormView.setCardValidCallback { isValid, invalidFields ->
            Log.d("CardForView", "isValid: $isValid, invalidFields: $invalidFields")
            getParamButton.isEnabled = isValid
        }

        toggleButton.setOnClickListener {
            cardFormView.isEnabled = !cardFormView.isEnabled
        }

        getParamButton.setOnClickListener {
            val cardParams = cardFormView.cardParams
            Log.d("CardForView", "params: $cardParams")
            result.text = "$cardParams"
        }
    }

    abstract fun initializeViews()
}