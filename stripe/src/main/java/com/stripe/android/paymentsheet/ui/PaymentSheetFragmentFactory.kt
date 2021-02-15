package com.stripe.android.paymentsheet.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.paymentsheet.PaymentOptionsAddCardFragment
import com.stripe.android.paymentsheet.PaymentOptionsListFragment
import com.stripe.android.paymentsheet.PaymentSheetAddCardFragment
import com.stripe.android.paymentsheet.PaymentSheetLoadingFragment
import com.stripe.android.paymentsheet.PaymentSheetPaymentMethodsListFragment
import com.stripe.android.paymentsheet.analytics.EventReporter

internal class PaymentSheetFragmentFactory(
    private val eventReporter: EventReporter
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            PaymentOptionsListFragment::class.java.name -> {
                PaymentOptionsListFragment(eventReporter)
            }
            PaymentSheetPaymentMethodsListFragment::class.java.name -> {
                PaymentSheetPaymentMethodsListFragment(eventReporter)
            }
            PaymentSheetAddCardFragment::class.java.name -> {
                PaymentSheetAddCardFragment(eventReporter)
            }
            PaymentOptionsAddCardFragment::class.java.name -> {
                PaymentOptionsAddCardFragment(eventReporter)
            }
            PaymentSheetLoadingFragment::class.java.name -> {
                PaymentSheetLoadingFragment()
            }
            else -> {
                super.instantiate(classLoader, className)
            }
        }
    }
}
