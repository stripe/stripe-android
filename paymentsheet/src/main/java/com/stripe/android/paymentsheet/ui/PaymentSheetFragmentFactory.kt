package com.stripe.android.paymentsheet.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.paymentsheet.PaymentOptionsAddPaymentMethodFragment
import com.stripe.android.paymentsheet.PaymentOptionsListFragment
import com.stripe.android.paymentsheet.PaymentSheetAddPaymentMethodFragment
import com.stripe.android.paymentsheet.PaymentSheetListFragment
import com.stripe.android.paymentsheet.analytics.EventReporter

internal class PaymentSheetFragmentFactory(
    private val eventReporter: EventReporter
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            PaymentOptionsListFragment::class.java.name -> {
                PaymentOptionsListFragment(eventReporter)
            }
            PaymentSheetListFragment::class.java.name -> {
                PaymentSheetListFragment(eventReporter)
            }
            PaymentSheetAddPaymentMethodFragment::class.java.name -> {
                PaymentSheetAddPaymentMethodFragment(eventReporter)
            }
            PaymentOptionsAddPaymentMethodFragment::class.java.name -> {
                PaymentOptionsAddPaymentMethodFragment(eventReporter)
            }
            else -> {
                super.instantiate(classLoader, className)
            }
        }
    }
}
