package com.stripe.example.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.DrawableRes
import androidx.lifecycle.Observer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.databinding.DropdownMenuPopupItemBinding
import com.stripe.example.databinding.SimplePaymentMethodActivityBinding

class SimplePaymentMethodConfirmationActivity : StripeIntentActivity() {

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

    private fun onDropdownItemSelected() {
        val dropdownItem = this.dropdownItem
        viewBinding.nameLayout.visibility = viewVisibility(dropdownItem.requiresName)
        viewBinding.emailLayout.visibility = viewVisibility(dropdownItem.requiresEmail)
        viewBinding.paymentMethod.setCompoundDrawablesRelativeWithIntrinsicBounds(
            dropdownItem.icon, 0, 0, 0
        )
    }

    private fun viewVisibility(visible: Boolean): Int {
        return if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        val adapter = DropdownItemAdapter(this)
        viewBinding.paymentMethod.setAdapter(adapter)
        viewBinding.paymentMethod.setOnItemClickListener { _, _, _, _ ->
            viewModel.status.value = ""
            onDropdownItemSelected()
        }
        viewBinding.paymentMethod.setText(DropdownItem.P24.name, false)
        onDropdownItemSelected()

        viewBinding.payNow.setOnClickListener {
            createAndConfirmPaymentIntent(dropdownItem.country, paymentMethodCreateParams)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_EMAIL, viewBinding.email.text.toString())
        outState.putString(STATE_NAME, viewBinding.name.text.toString())
        outState.putString(STATE_DROPDOWN_ITEM, viewBinding.paymentMethod.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getString(STATE_EMAIL)?.let(viewBinding.email::setText)
        savedInstanceState.getString(STATE_NAME)?.let(viewBinding.name::setText)
        savedInstanceState.getString(STATE_DROPDOWN_ITEM)?.let {
            viewBinding.paymentMethod.setText(it)
            onDropdownItemSelected()
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.payNow.isEnabled = enabled
        viewBinding.name.isEnabled = enabled
        viewBinding.email.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }

    companion object {
        private const val STATE_NAME = "name"
        private const val STATE_EMAIL = "email"
        private const val STATE_DROPDOWN_ITEM = "dropdown_item"

        private enum class DropdownItem(
            val country: String,
            @DrawableRes val icon: Int,
            val createParams: (PaymentMethod.BillingDetails, Map<String, String>?) -> PaymentMethodCreateParams,
            val requiresName: Boolean = true,
            val requiresEmail: Boolean = false
        ) {
            P24(
                "pl",
                R.drawable.ic_brandicon__p24,
                PaymentMethodCreateParams.Companion::createP24,
                requiresName = false, requiresEmail = true
            ),
            Bancontact(
                "be", R.drawable.ic_brandicon__bancontact,
                PaymentMethodCreateParams.Companion::createBancontact
            ),
            EPS(
                "at", R.drawable.ic_brandicon__eps,
                PaymentMethodCreateParams.Companion::createEps
            ),
            Giropay(
                "de", R.drawable.ic_brandicon__giropay,
                PaymentMethodCreateParams.Companion::createGiropay
            ),
            GrabPay(
                "sg", R.drawable.ic_brandicon_grabpay,
                PaymentMethodCreateParams.Companion::createGrabPay
            );
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

            /*
            The material ExposedDropdownMenu abuses an AutocompleteTextView as a Spinner.
            When we want to set the selected item, the AutocompleteTextView tries to
            filter the results to only those that start with that item.
            We do not want this behaviour so we ignore AutocompleteTextView's filtering logic.
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
                    dropdownItem.icon, 0, 0, 0
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
}
