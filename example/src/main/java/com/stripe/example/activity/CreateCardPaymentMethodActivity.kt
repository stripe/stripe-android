package com.stripe.example.activity

import android.os.Bundle
import android.util.Log
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

        viewBinding.cardFormView.setCardValidCallback { isValid, invalidFields ->
            viewBinding.createButton.isEnabled = isValid
            Log.d(
                CARD_VALID_CALLBACK_TAG,
                "Card information is " + (if (isValid) " valid" else " invalid")
            )
            if (!isValid) {
                Log.d(CARD_VALID_CALLBACK_TAG, " Invalid fields are $invalidFields")
            }
        }

        viewBinding.createButton.setOnClickListener {
            keyboardController.hide()

            viewBinding.cardFormView.cardParams?.let {
                createPaymentMethod(PaymentMethodCreateParams.createCard(it))
            }
        }

        viewBinding.toggleCardFormView.setOnClickListener {
            viewBinding.cardFormView.isEnabled = !viewBinding.cardFormView.isEnabled
            showSnackbar(
                if (viewBinding.cardFormView.isEnabled) {
                    "CardFormView Enabled"
                } else {
                    "CardFormView Disabled"
                }
            )
        }
    }

    private fun createPaymentMethod(params: PaymentMethodCreateParams) {
        onCreatePaymentMethodStart()
        viewModel.createPaymentMethod(params).observe(this) { result ->
            onCreatePaymentMethodCompleted()
            result.fold(
                onSuccess = ::onCreatedPaymentMethod,
                onFailure = {
                    showSnackbar(it.message.orEmpty())
                }
            )
        }
    }

    private fun showSnackbar(message: String) {
        snackbarController.show(message)
    }

    private fun onCreatePaymentMethodStart() {
        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.createButton.isEnabled = false
    }

    private fun onCreatePaymentMethodCompleted() {
        viewBinding.progressBar.visibility = View.INVISIBLE
        viewBinding.createButton.isEnabled = true
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

    private companion object {
        private const val CARD_VALID_CALLBACK_TAG = "CardValidCallback"
    }
}
