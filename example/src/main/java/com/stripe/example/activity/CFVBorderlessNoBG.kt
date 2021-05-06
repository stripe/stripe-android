package com.stripe.example.activity

import com.stripe.example.databinding.CfvBorderlessNoBgBinding

class CFVBorderlessNoBG : BaseCFVActvity() {

    override fun initializeViews() {
        val binding = CfvBorderlessNoBgBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cardFormView = binding.cardFormView
        getParamButton = binding.cardParams
        toggleButton = binding.toggle
        result = binding.result
    }
}
