package com.stripe.example.activity

import com.stripe.example.databinding.CfvBorderlessBgBinding

class CFVBorderlessBG : BaseCFVActvity() {

    override fun initializeViews() {
        val binding = CfvBorderlessBgBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cardFormView = binding.cardFormView
        getParamButton = binding.cardParams
        toggleButton = binding.toggle
        result = binding.result
    }
}
