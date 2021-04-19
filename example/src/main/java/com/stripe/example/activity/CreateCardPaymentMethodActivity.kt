package com.stripe.example.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.databinding.CreateCardPaymentMethodActivityBinding
import com.stripe.example.databinding.PaymentMethodItemBinding

class CreateCardPaymentMethodActivity : AppCompatActivity() {
    private val viewBinding: CreateCardPaymentMethodActivityBinding by lazy {
        CreateCardPaymentMethodActivityBinding.inflate(layoutInflater)
    }

    private val viewModel: PaymentMethodViewModel by viewModels()

    private val adapter: PaymentMethodsAdapter = PaymentMethodsAdapter()
    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.paymentMethods.setHasFixedSize(false)
        viewBinding.paymentMethods.layoutManager = LinearLayoutManager(this)
        viewBinding.paymentMethods.adapter = adapter

        viewBinding.cardMultilineWidget.setCardValidCallback { isValid, invalidFields ->
            // added as an example - no-op
        }

        viewBinding.createButton.setOnClickListener {
            keyboardController.hide()

            viewBinding.cardMultilineWidget.paymentMethodCreateParams?.let {
                createPaymentMethod(it, viewBinding.useSuspendApi.isChecked)
            }
        }
    }

    private fun createPaymentMethod(params: PaymentMethodCreateParams, useSuspendApi: Boolean) {
        onCreatePaymentMethodStart()
        viewModel.createPaymentMethod(params, useSuspendApi).observe(
            this,
            { result ->
                onCreatePaymentMethodCompleted()
                result.fold(
                    onSuccess = ::onCreatedPaymentMethod,
                    onFailure = {
                        showSnackbar(it.message.orEmpty())
                    }
                )
            }
        )
    }

    private fun showSnackbar(message: String) {
        snackbarController.show(message)
    }

    private fun onCreatePaymentMethodStart() {
        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.createButton.isEnabled = false
        viewBinding.useSuspendApi.isEnabled = false
    }

    private fun onCreatePaymentMethodCompleted() {
        viewBinding.progressBar.visibility = View.INVISIBLE
        viewBinding.createButton.isEnabled = true
        viewBinding.useSuspendApi.isEnabled = true
    }

    private fun onCreatedPaymentMethod(paymentMethod: PaymentMethod?) {
        if (paymentMethod != null) {
            adapter.paymentMethods.add(0, paymentMethod)
            adapter.notifyItemInserted(0)
        } else {
            showSnackbar("Created null PaymentMethod")
        }
    }

    private class PaymentMethodsAdapter :
        RecyclerView.Adapter<PaymentMethodsAdapter.PaymentMethodViewHolder>() {
        val paymentMethods: MutableList<PaymentMethod> = mutableListOf()

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
            return PaymentMethodViewHolder(
                PaymentMethodItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return paymentMethods.size
        }

        override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
            holder.setPaymentMethod(paymentMethods[position])
        }

        override fun getItemId(position: Int): Long {
            return requireNotNull(paymentMethods[position].id).hashCode().toLong()
        }

        class PaymentMethodViewHolder internal constructor(
            private val viewBinding: PaymentMethodItemBinding
        ) : RecyclerView.ViewHolder(viewBinding.root) {
            internal fun setPaymentMethod(paymentMethod: PaymentMethod) {
                val card = paymentMethod.card
                viewBinding.paymentMethodId.text = paymentMethod.id
                viewBinding.brand.text = card?.brand?.displayName.orEmpty()
                viewBinding.last4.text = card?.last4.orEmpty()
            }
        }
    }
}
