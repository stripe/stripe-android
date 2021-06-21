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
import androidx.lifecycle.map
import com.stripe.android.paymentsheet.StripeTheme
import com.stripe.android.paymentsheet.forms.Form
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.specifications.sofort

class SofortDataCollectionFragment : BasePaymentDataCollectionFragment() {
    val sofortFormViewModel: FormViewModel by viewModels { FormViewModel.Factory(sofort.layout) }

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
    }

    override fun setProcessing(processing: Boolean) {
        // Disable views
    }

    // This can't be a var or we'll be reading from the ViewModel while the fragment is detached
    override fun paramMapLiveData() = sofortFormViewModel.completeFormValues.asLiveData().map { mutableMap ->
        mutableMap?.fieldValuePairs?.toMap()
    }
}
