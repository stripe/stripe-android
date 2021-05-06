package com.stripe.example.activity

import com.stripe.example.databinding.CfvBorderBgBinding

class CFVBorderBG : BaseCFVActvity() {

    override fun initializeViews() {
        val binding = CfvBorderBgBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cardFormView = binding.cardFormView
        getParamButton = binding.cardParams
        toggleButton = binding.toggle
        result = binding.result
    }
}
