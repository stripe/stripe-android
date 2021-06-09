package com.stripe.android.paymentsheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.forms.FormDataObjectReader
import com.stripe.android.paymentsheet.forms.FormViewModel

class FormFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val formDataObject = com.stripe.android.paymentsheet.forms.sofort

        val formViewModel = FormViewModel(
            formDataObject.paramKey,
            formDataObject.allTypes
        )

        formViewModel.paramMapFlow.asLiveData().observe(viewLifecycleOwner) {
            Log.e("APP", "Params: $it")
        }

        setContent {
            StripeTheme {
                Column(Modifier.fillMaxSize()) {
                    FormDataObjectReader(
                        formDataObject,
                        formViewModel
                    )
                }
            }
        }
    }
}
