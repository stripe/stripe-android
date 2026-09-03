package com.stripe.android.paymentsheet.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.fadeOut

internal abstract class BaseSheetActivity<ResultType> : AppCompatActivity() {
    abstract val viewModel: BaseSheetViewModel

    val linkHandler: LinkHandler
        get() = viewModel.linkHandler

    abstract fun setActivityResult(result: ResultType)

    // User keys identify the Stripe Dashboard mobile app, where MOTO lets merchants enter a customer's card.
    // Exclude those card details from Autofill so they cannot be saved on the merchant's device.
    protected fun disableAutofillForUserKey() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PaymentConfiguration.getInstance(this).isUserKey()) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        renderEdgeToEdge()

        onBackPressedDispatcher.addCallback {
            viewModel.handleBackPressed()
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
