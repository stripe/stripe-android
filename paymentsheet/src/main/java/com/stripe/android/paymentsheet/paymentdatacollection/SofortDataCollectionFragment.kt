package com.stripe.android.paymentsheet.paymentdatacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import com.stripe.android.paymentsheet.StripeTheme
import com.stripe.android.paymentsheet.forms.Form
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.forms.sofort

class SofortDataCollectionFragment : BasePaymentDataCollectionFragment() {
    val sofortFormViewModel: FormViewModel by viewModels { FormViewModel.Factory(sofort) }

    @ExperimentalAnimationApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setContent {
            StripeTheme {
                Column(Modifier.fillMaxSize()) {
                    Form(sofortFormViewModel)
                }
            }
        }

        sofortFormViewModel.paramMapFlow.asLiveData().observe(viewLifecycleOwner) {
            dataCollectionViewModel.formData.value = it
        }

        dataCollectionViewModel.processing.observe(viewLifecycleOwner) {
            // Disable views
        }
    }
}
