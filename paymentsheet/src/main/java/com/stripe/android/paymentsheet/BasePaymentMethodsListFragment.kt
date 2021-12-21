package com.stripe.android.paymentsheet

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal abstract class BasePaymentMethodsListFragment(
    private val canClickSelectedItem: Boolean
) : Fragment(
    R.layout.fragment_paymentsheet_payment_methods_list
) {
    abstract val sheetViewModel: BaseSheetViewModel<*>

    protected lateinit var config: FragmentConfig
    private lateinit var adapter: PaymentOptionsAdapter
    private var editMenuItem: MenuItem? = null

    @VisibleForTesting
    internal var isEditing = false
        set(value) {
            field = value
            adapter.setEditing(value)
            editMenuItem?.setTitle(if (value) R.string.done else R.string.edit)
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
        sheetViewModel.eventReporter.onShowExistingPaymentOptions()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(FragmentPaymentsheetPaymentMethodsListBinding.bind(view))
        isEditing = savedInstanceState?.getBoolean(IS_EDITING) ?: false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.paymentsheet_payment_methods_list, menu)
        // Menu is created after view state is restored, so we need to update the title here
        editMenuItem = menu.findItem(R.id.edit).apply {
            setTitle(if (isEditing) R.string.done else R.string.edit)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

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
            canClickSelectedItem,
            paymentOptionSelectedListener = ::onPaymentOptionSelected,
            paymentMethodDeleteListener = ::deletePaymentMethod,
            addCardClickListener = {
                transitionToAddPaymentMethod()
            }
        ).also {
            viewBinding.recycler.adapter = it
        }

        adapter.setItems(
            config,
            sheetViewModel.paymentMethods.value.orEmpty(),
            sheetViewModel.selection.value
        )

        sheetViewModel.processing.observe(viewLifecycleOwner) { isProcessing ->
            adapter.isEnabled = !isProcessing
            layoutManager.canScroll = !isProcessing
        }
    }

    abstract fun transitionToAddPaymentMethod()

    open fun onPaymentOptionSelected(
        paymentSelection: PaymentSelection,
        isClick: Boolean
    ) {
        sheetViewModel.updateSelection(paymentSelection)
    }

    private fun deletePaymentMethod(item: PaymentOptionsAdapter.Item.SavedPaymentMethod) =
        AlertDialog.Builder(requireActivity())
            .setTitle(
                resources.getString(
                    R.string.stripe_paymentsheet_remove_pm,
                    SupportedPaymentMethod.fromCode(item.paymentMethod.type?.code)
                        ?.run {
                            resources.getString(
                                displayNameResource
                            )
                        }
                )
            )
            .setMessage(item.getDescription(resources))
            .setCancelable(true)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.remove) { _, _ ->
                adapter.removeItem(item)
                sheetViewModel.removePaymentMethod(item.paymentMethod)
            }
            .create()
            .show()

    private companion object {
        private const val IS_EDITING = "is_editing"
    }
}
