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
import androidx.fragment.app.activityViewModels
import com.stripe.android.paymentsheet.StripeTheme
import com.stripe.android.paymentsheet.forms.Form
import com.stripe.android.paymentsheet.forms.FormViewModel

internal class IdealFragment : BasePaymentDataCollectionFragment() {
    private val viewModel2 by activityViewModels<FormViewModel>()

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
                    Form(viewModel2)
                }
            }
        }
    }
}
