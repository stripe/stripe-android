package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsThemeDefaults
import com.stripe.android.ui.core.createTextSpanFromTextStyle
import com.stripe.android.ui.core.isSystemDarkTheme
import kotlinx.coroutines.launch

internal abstract class BasePaymentMethodsListFragment : Fragment() {
    abstract val sheetViewModel: BaseSheetViewModel<*>

    protected lateinit var config: FragmentConfig
    private var editMenuItem: MenuItem? = null

    private val menuProvider = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.paymentsheet_payment_methods_list, menu)
        }

        override fun onPrepareMenu(menu: Menu) {
            editMenuItem = menu.findItem(R.id.edit)
            setEditMenuItemText(isEditing = false)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.edit -> {
                    sheetViewModel.setEditing(!sheetViewModel.isEditing.value)
                }
                else -> Unit
            }

            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nullableConfig = arguments?.getParcelable<FragmentConfig>(
            BaseSheetActivity.EXTRA_FRAGMENT_CONFIG
        )
        if (nullableConfig == null) {
            sheetViewModel.onFatal(
                IllegalArgumentException("Failed to start existing payment options fragment.")
            )
            return
        }
        this.config = nullableConfig

        requireActivity().addMenuProvider(menuProvider)

        sheetViewModel.eventReporter.onShowExistingPaymentOptions(
            linkEnabled = sheetViewModel.isLinkEnabled.value ?: false,
            activeLinkSession = sheetViewModel.activeLinkSession.value ?: false
        )

        sheetViewModel.paymentMethods.observe(this) { paymentMethods ->
            editMenuItem?.isVisible = paymentMethods.isNotEmpty()
            if (!isVisible) {
                sheetViewModel.setEditing(false)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sheetViewModel.isEditing.collect { isEditing ->
                    setEditMenuItemText(isEditing)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = ComposeView(requireContext())

        view.setContent {
            PaymentOptions(
                sheetViewModel = sheetViewModel,
                transitionToAddPaymentMethod = this::transitionToAddPaymentMethod,
                modifier = Modifier
                    .padding(
                        top = dimensionResource(
                            id = R.dimen.stripe_paymentsheet_paymentoptions_margin_top,
                        ),
                        bottom = dimensionResource(
                            id = R.dimen.stripe_paymentsheet_paymentoptions_margin_bottom,
                        ),
                    )
            )
        }

        return view
    }

    private fun setEditMenuItemText(isEditing: Boolean) {
        val context = context ?: return
        val appearance = sheetViewModel.config?.appearance ?: return

        editMenuItem?.apply {
            title = createTextSpanFromTextStyle(
                text = getString(if (isEditing) R.string.done else R.string.edit),
                context = context,
                fontSizeDp = (
                    appearance.typography.sizeScaleFactor
                        * PaymentsThemeDefaults.typography.smallFontSize.value
                    ).dp,
                color = Color(appearance.getColors(context.isSystemDarkTheme()).appBarIcon),
                fontFamily = appearance.typography.fontResId
            )
        }
    }

    abstract fun transitionToAddPaymentMethod()
}
