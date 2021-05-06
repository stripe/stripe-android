package com.stripe.example.activity

import com.stripe.example.databinding.CfvBorderNoBgBinding

class CFVBorderNoBG : BaseCFVActvity() {

    override fun initializeViews() {
        val binding = CfvBorderNoBgBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cardFormView = binding.cardFormView
        getParamButton = binding.cardParams
        toggleButton = binding.toggle
        result = binding.result
    }
}
