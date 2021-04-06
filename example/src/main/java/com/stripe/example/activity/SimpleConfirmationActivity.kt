package com.stripe.example.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.databinding.DropdownMenuPopupItemBinding
import com.stripe.example.databinding.SimplePaymentMethodActivityBinding

class SimpleConfirmationActivity : StripeIntentActivity() {
    private val simpleConfirmationViewModel: SimpleConfirmationViewModel by viewModels()

    private val viewBinding: SimplePaymentMethodActivityBinding by lazy {
        SimplePaymentMethodActivityBinding.inflate(layoutInflater)
    }

    private val dropdownItem: DropdownItem
        get() {
            return DropdownItem.valueOf(
                viewBinding.paymentMethod.text.toString()
            )
        }

    private val paymentMethodCreateParams: PaymentMethodCreateParams
        get() {
            val dropdownItem = this.dropdownItem
            val billingDetails = PaymentMethod.BillingDetails(
                name = viewBinding.name.text.toString().takeIf { dropdownItem.requiresName },
                email = viewBinding.email.text.toString().takeIf { dropdownItem.requiresEmail }
            )
            return dropdownItem.createParams(billingDetails, null)
        }

    private fun onPaymentMethodSelected() {
        val dropdownItem = this.dropdownItem

        simpleConfirmationViewModel.selectedPaymentMethod = dropdownItem.name

        viewBinding.nameLayout.isVisible = dropdownItem.requiresName
        viewBinding.emailLayout.isVisible = dropdownItem.requiresEmail
        viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
            dropdownItem.icon,
            0,
            0,
            0
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this) { enableUi(!it) }
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        val adapter = DropdownItemAdapter(this)
        viewBinding.paymentMethod.setAdapter(adapter)
        viewBinding.paymentMethod.setOnItemClickListener { _, _, _, _ ->
            viewModel.status.value = ""
            onPaymentMethodSelected()
        }
        viewBinding.paymentMethod.setText(DropdownItem.P24.name, false)

        viewBinding.payNow.setOnClickListener {
            createAndConfirmPaymentIntent(dropdownItem.country, paymentMethodCreateParams)
        }

        viewBinding.name.setText(simpleConfirmationViewModel.name)
        viewBinding.email.setText(simpleConfirmationViewModel.email)

        viewBinding.name.doAfterTextChanged {
            simpleConfirmationViewModel.email = it?.toString().orEmpty()
        }
        viewBinding.email.doAfterTextChanged {
            simpleConfirmationViewModel.email = it?.toString().orEmpty()
        }

        simpleConfirmationViewModel.selectedPaymentMethod?.let {
            viewBinding.paymentMethod.setText(it)
        }
        onPaymentMethodSelected()
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.payNow.isEnabled = enabled
        viewBinding.name.isEnabled = enabled
        viewBinding.email.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }

    internal class SimpleConfirmationViewModel : ViewModel() {
        var name = "Jenny Rosen"
        var email = "jrosen@example.com"
        var selectedPaymentMethod: String? = null
    }

    private enum class DropdownItem(
        val country: String,
        @DrawableRes val icon: Int,
        val createParams:
            (PaymentMethod.BillingDetails, Map<String, String>?) -> PaymentMethodCreateParams,
        val requiresName: Boolean = true,
        val requiresEmail: Boolean = false
    ) {
        P24(
            "pl",
            R.drawable.ic_brandicon__p24,
            PaymentMethodCreateParams.Companion::createP24,
            requiresName = false,
            requiresEmail = true
        ),
        Bancontact(
            "be",
            R.drawable.ic_brandicon__bancontact,
            PaymentMethodCreateParams.Companion::createBancontact
        ),
        EPS(
            "at",
            R.drawable.ic_brandicon__eps,
            PaymentMethodCreateParams.Companion::createEps
        ),
        Giropay(
            "de",
            R.drawable.ic_brandicon__giropay,
            PaymentMethodCreateParams.Companion::createGiropay
        ),
        GrabPay(
            "sg",
            R.drawable.ic_brandicon_grabpay,
            PaymentMethodCreateParams.Companion::createGrabPay
        ),
        Oxxo(
            "mx",
            R.drawable.ic_brandicon__oxxo,
            PaymentMethodCreateParams.Companion::createOxxo,
            requiresEmail = true
        )
    }

    private class DropdownItemAdapter(
        context: Context
    ) : ArrayAdapter<DropdownItem>(
        context,
        0
    ) {
        private val layoutInflater = LayoutInflater.from(context)

        init {
            addAll(*DropdownItem.values())
        }

        /**
         * The material ExposedDropdownMenu abuses an AutocompleteTextView as a Spinner.
         * When we want to set the selected item, the AutocompleteTextView tries to
         * filter the results to only those that start with that item.
         * We do not want this behaviour so we ignore AutocompleteTextView's filtering logic.
         */
        override fun getFilter(): Filter {
            return NullFilter
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val viewBinding = convertView?.let {
                DropdownMenuPopupItemBinding.bind(convertView)
            } ?: DropdownMenuPopupItemBinding.inflate(layoutInflater, parent, false)

            val dropdownItem = requireNotNull(getItem(position))
            viewBinding.text.text = dropdownItem.name
            viewBinding.text.setCompoundDrawablesRelativeWithIntrinsicBounds(
                dropdownItem.icon,
                0,
                0,
                0
            )

            return viewBinding.root
        }

        private object NullFilter : Filter() {
            private val emptyResult = FilterResults()

            override fun performFiltering(prefix: CharSequence?): FilterResults {
                return emptyResult
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            }
        }
    }
}
