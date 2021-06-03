package com.stripe.android.paymentsheet.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.paymentsheet.AddCardFragment
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

/**
 * [FragmentFactory] for fragments used to add new payment methods.
 */
internal class PaymentMethodsFragmentFactory<ViewModelType : BaseSheetViewModel<*>>(
    private val viewModelClass: Class<ViewModelType>,
    private val viewModelFactory: ViewModelProvider.Factory
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            AddCardFragment::class.java.name -> {
                AddCardFragment(viewModelClass, viewModelFactory)
            }
            else -> super.instantiate(classLoader, className)
        }
    }
}
