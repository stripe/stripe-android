package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsThemeDefaults
import com.stripe.android.ui.core.createTextSpanFromTextStyle
import com.stripe.android.ui.core.isSystemDarkTheme
import kotlinx.coroutines.launch

internal abstract class BasePaymentMethodsListFragment(
    private val canClickSelectedItem: Boolean
) : Fragment(
    R.layout.fragment_paymentsheet_payment_methods_list
) {
    abstract val sheetViewModel: BaseSheetViewModel<*>

    @VisibleForTesting
    lateinit var adapter: PaymentOptionsAdapter
    protected lateinit var config: FragmentConfig
    private var editMenuItem: MenuItem? = null

    @VisibleForTesting
    internal var isEditing = false
        set(value) {
            field = value
            adapter.setEditing(value)
            setEditMenuText()
            sheetViewModel.setEditing(value)
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

        setHasOptionsMenu(!sheetViewModel.paymentMethods.value.isNullOrEmpty())
        sheetViewModel.eventReporter.onShowExistingPaymentOptions(
            linkEnabled = sheetViewModel.isLinkEnabled.value ?: false,
            activeLinkSession = sheetViewModel.activeLinkSession.value ?: false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(FragmentPaymentsheetPaymentMethodsListBinding.bind(view))
        isEditing = savedInstanceState?.getBoolean(IS_EDITING) ?: false
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.paymentsheet_payment_methods_list, menu)
        // Menu is created after view state is restored, so we need to update the title here
        editMenuItem = menu.findItem(R.id.edit)
        setEditMenuText()
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setEditMenuText() {
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
            isVisible = adapter.hasSavedItems()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit -> {
                isEditing = !isEditing
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(IS_EDITING, isEditing)
        super.onSaveInstanceState(outState)
    }

    private fun setupRecyclerView(viewBinding: FragmentPaymentsheetPaymentMethodsListBinding) {
        val layoutManager = object : LinearLayoutManager(
            activity,
            HORIZONTAL,
            false
        ) {
            var canScroll = true

            override fun canScrollHorizontally(): Boolean {
                return canScroll && super.canScrollHorizontally()
            }
        }.also {
            viewBinding.recycler.layoutManager = it
        }

        adapter = PaymentOptionsAdapter(
            lpmRepository = sheetViewModel.lpmResourceRepository.getRepository(),
            canClickSelectedItem = canClickSelectedItem,
            paymentOptionSelected = ::onPaymentOptionsItemSelected,
            paymentMethodDeleteListener = ::deletePaymentMethod,
            addCardClickListener = ::transitionToAddPaymentMethod
        ).also {
            viewBinding.recycler.adapter = it
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sheetViewModel.paymentOptionsState.collect { paymentOptionsState ->
                    adapter.update(
                        items = paymentOptionsState.items,
                        selectedIndex = paymentOptionsState.selectedIndex,
                    )
                }
            }
        }

        sheetViewModel.paymentMethods.observe(viewLifecycleOwner) { paymentMethods ->
            if (isEditing && paymentMethods.isEmpty()) {
                isEditing = false
            }
        }

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            adapter.isEnabled = !isProcessing
            layoutManager.canScroll = !isProcessing
        }
    }

    abstract fun transitionToAddPaymentMethod()

    open fun onPaymentOptionsItemSelected(item: PaymentOptionsItem) {
        val paymentSelection = item.toPaymentSelection()
        sheetViewModel.updateSelection(paymentSelection)
    }

    @VisibleForTesting
    fun deletePaymentMethod(item: PaymentOptionsItem.SavedPaymentMethod) {
        sheetViewModel.removePaymentMethod(item.paymentMethod)
    }

    private companion object {
        private const val IS_EDITING = "is_editing"
    }
}
