package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.PaymentsThemeDefaults
import com.stripe.android.ui.core.createTextSpanFromTextStyle
import com.stripe.android.ui.core.isSystemDarkTheme

internal abstract class BasePaymentMethodsListFragment(
    private val canClickSelectedItem: Boolean
) : Fragment(
    R.layout.fragment_paymentsheet_payment_methods_list
) {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(view)
        setContent(viewBinding)
    }

    private fun setContent(viewBinding: FragmentPaymentsheetPaymentMethodsListBinding) {
        viewBinding.content.setContent {
            PaymentOptionsScreen(
                sheetViewModel = sheetViewModel,
                canClickSelectedItem = canClickSelectedItem,
                toggleEditMenuItem = this::toggleEditMenuItem,
                setEditMenuItemText = this::setEditMenuItemText,
                transitionToAddPaymentMethod = this::transitionToAddPaymentMethod,
            )
        }
    }

    private fun toggleEditMenuItem(isVisible: Boolean) {
        editMenuItem?.isVisible = isVisible
        if (!isVisible) {
            sheetViewModel.setEditing(false)
        }
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

@Composable
private fun PaymentOptionsScreen(
    sheetViewModel: BaseSheetViewModel<*>,
    canClickSelectedItem: Boolean,
    toggleEditMenuItem: (Boolean) -> Unit,
    setEditMenuItemText: (Boolean) -> Unit,
    transitionToAddPaymentMethod: () -> Unit,
) {
    val context = LocalContext.current

    val state by sheetViewModel.paymentOptionsState.collectAsState()
    val isProcessing by sheetViewModel.processing.observeAsState(initial = false)

    val savedPaymentMethods by sheetViewModel.paymentMethods.observeAsState(initial = emptyList())
    val isEditing by sheetViewModel.isEditing.collectAsState()

    LaunchedEffect(savedPaymentMethods) {
        val setVisible = savedPaymentMethods.isNotEmpty()
        toggleEditMenuItem(setVisible)
    }

    LaunchedEffect(isEditing) {
        setEditMenuItemText(isEditing)
    }

    val lpmRepository = remember(sheetViewModel) {
        sheetViewModel.lpmResourceRepository.getRepository()
    }

    PaymentsTheme {
        PaymentOptions(
            state = state,
            isEnabled = !isProcessing,
            isEditing = isEditing,
            paymentMethodNameProvider = { code ->
                lpmRepository.fromCode(code)?.let {
                    context.getString(it.displayNameResource)
                }
            },
            onAddCard = transitionToAddPaymentMethod,
            onRemove = { item ->
                sheetViewModel.removePaymentMethod(item.paymentMethod)
            },
            onItemSelected = { item ->
                val isAllowed = canClickSelectedItem || item != state.selectedItem
                if (isAllowed) {
                    val paymentSelection = item.toPaymentSelection()
                    sheetViewModel.updateSelection(paymentSelection)
                    (sheetViewModel as? PaymentOptionsViewModel)?.onUserSelection()
                }
            },
        )
    }
}
