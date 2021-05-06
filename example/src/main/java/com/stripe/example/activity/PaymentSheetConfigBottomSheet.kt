package com.stripe.example.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.example.R
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

        childFragmentManager
            .beginTransaction()
            .replace(
                viewBinding.fragmentContainer.id,
                PreferenceFragment()
            )
            .commit()
    }

    internal class PreferenceFragment : PreferenceFragmentCompat() {
        var enableCustomerApi: SwitchPreferenceCompat? = null
        var isReturningCustomer: SwitchPreferenceCompat? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.payment_sheet_preferences, rootKey)

            enableCustomerApi = findPreference("enable_customer")
            isReturningCustomer = findPreference("returning_customer")
            enableCustomerApi?.setOnPreferenceClickListener {
                updatePreferencesVisibility()
                false
            }
            updatePreferencesVisibility()
        }

        private fun updatePreferencesVisibility() {
            isReturningCustomer?.isVisible = enableCustomerApi?.isChecked == true
        }
    }

    internal companion object {
        val TAG = PaymentSheetConfigBottomSheet::class.java.simpleName
    }
}
