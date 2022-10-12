package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.android.ui.core.PaymentsTheme

internal class PollingFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PaymentsTheme {
                    Text(
                        text = "Coming soon 🚧",
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }

    companion object {
        const val KEY_FRAGMENT_RESULT = "KEY_FRAGMENT_RESULT_PollingFragment"

        fun newInstance(): PollingFragment {
            return PollingFragment()
        }
    }
}
