package com.stripe.example.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.stripe.example.databinding.ToyActivityBinding

class ToyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ToyActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardFormView.setCardValidCallback { isValid, invalidFields ->
            Log.d("CardForView", "isValid: $isValid, invalidFields: $invalidFields")
            binding.cardParams.isEnabled = isValid
        }

        binding.toggle.setOnClickListener {
            binding.cardFormView.isEnabled = !binding.cardFormView.isEnabled
        }



        binding.cardParams.setOnClickListener {
            val cardParams = binding.cardFormView.cardParams
            Log.d("CardForView", "params: $cardParams")
        }
    }
}