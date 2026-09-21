package com.stripe.android.paymentsheet.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
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
    @SuppressLint("InlinedApi")
    protected fun disableAutofillForUserKey() {
        val isUserKey = try {
            PaymentConfiguration.getInstance(this).isUserKey()
        } catch (_: IllegalStateException) {
            // getInstance() throws when PaymentConfiguration is unavailable; default to preserving Autofill.
            false
        }

        if (isUserKey) {
            ViewCompat.setImportantForAutofill(
                window.decorView,
                View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,
            )
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
