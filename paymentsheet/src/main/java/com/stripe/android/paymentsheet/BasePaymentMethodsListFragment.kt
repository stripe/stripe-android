package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.createTextSpanFromTextStyle
import com.stripe.android.uicore.isSystemDarkTheme

internal abstract class BasePaymentMethodsListFragment : Fragment() {

    abstract val sheetViewModel: BaseSheetViewModel

    private var editMenuItem: MenuItem? = null
    private var viewBinding: FragmentPaymentsheetPaymentMethodsListBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(!sheetViewModel.paymentMethods.value.isNullOrEmpty())

        sheetViewModel.paymentMethods.launchAndCollectIn(this) { paymentMethods ->
            val hasPaymentMethods = paymentMethods.orEmpty().isNotEmpty()
            editMenuItem?.isVisible = hasPaymentMethods
        }

        sheetViewModel.editing.launchAndCollectIn(this) { isEditing ->
            setEditMenuText(isEditing)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentPaymentsheetPaymentMethodsListBinding
            .inflate(inflater, container, false)
        return viewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding?.content?.setContent {
            PaymentOptions(viewModel = sheetViewModel)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.paymentsheet_payment_methods_list, menu)
        // Menu is created after view state is restored, so we need to update the title here
        editMenuItem = menu.findItem(R.id.edit)
        setEditMenuText(isEditing = false)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setEditMenuText(isEditing: Boolean) {
        val context = context ?: return
        val appearance = sheetViewModel.config?.appearance ?: return
        editMenuItem?.apply {
            title = createTextSpanFromTextStyle(
                text = getString(if (isEditing) R.string.done else R.string.edit),
                context = context,
                fontSizeDp = (
                    appearance.typography.sizeScaleFactor
                        * StripeThemeDefaults.typography.smallFontSize.value
                    ).dp,
                color = Color(appearance.getColors(context.isSystemDarkTheme()).appBarIcon),
                fontFamily = appearance.typography.fontResId
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit -> {
                sheetViewModel.toggleEditing()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }
}
