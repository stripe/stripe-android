package com.stripe.example.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.example.databinding.PaymentSheetConfigBottomSheetBinding

class PaymentSheetConfigBottomSheet : BottomSheetDialogFragment() {
    private var _viewBinding: PaymentSheetConfigBottomSheetBinding? = null
    private val viewBinding get() = requireNotNull(_viewBinding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = PaymentSheetConfigBottomSheetBinding.inflate(
            requireActivity().layoutInflater,
            container,
            false
        )
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
