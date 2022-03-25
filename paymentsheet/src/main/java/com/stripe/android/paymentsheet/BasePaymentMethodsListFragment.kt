package com.stripe.android.paymentsheet

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.PaymentsThemeDefaults
import com.stripe.android.ui.core.convertDpToPx
import com.stripe.android.ui.core.isSystemDarkTheme

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
        editMenuItem = menu.findItem(R.id.edit)
        setEditMenuText()
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun setEditMenuText() {
        val isDark = this.context?.isSystemDarkTheme() ?: false
        editMenuItem?.apply {
            val editMenuText = getString(if (isEditing) R.string.done else R.string.edit)
            val editMenuTextSpan = SpannableString(editMenuText)

            sheetViewModel.config?.appearance?.let { appearance ->
                val color = Color(appearance.getColors(isDark).appBarIcon).toArgb()
                editMenuTextSpan.setSpan(
                    ForegroundColorSpan(color),
                    0,
                    editMenuTextSpan.length,
                    0
                )

                context?.let {
                    val typeFace = ResourcesCompat.getFont(
                        it,
                        appearance.typography.fontResId
                    )
                    typeFace?.let {
                        editMenuTextSpan.setSpan(
                            CustomTypefaceSpan(typeFace),
                            0,
                            editMenuTextSpan.length,
                            0
                        )
                    }

                    val fontSize = it.convertDpToPx(
                        (
                            PaymentsThemeDefaults.typography.smallFont
                                * appearance.typography.sizeScaleFactor
                            ).dp
                    )
                    editMenuTextSpan.setSpan(
                        AbsoluteSizeSpan(fontSize.toInt()),
                        0,
                        editMenuTextSpan.length,
                        0
                    )
                }
            }
            title = editMenuTextSpan
        }
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

    private fun deletePaymentMethod(item: PaymentOptionsAdapter.Item.SavedPaymentMethod) {
        adapter.removeItem(item)
        sheetViewModel.removePaymentMethod(item.paymentMethod)
    }

    class CustomTypefaceSpan(typeface: Typeface) : MetricAffectingSpan() {
        private val typeface: Typeface = typeface
        override fun updateDrawState(ds: TextPaint) {
            applyCustomTypeFace(ds, typeface)
        }

        override fun updateMeasureState(paint: TextPaint) {
            applyCustomTypeFace(paint, typeface)
        }

        companion object {
            private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
                paint.typeface = tf
            }
        }
    }

    private companion object {
        private const val IS_EDITING = "is_editing"
    }
}
