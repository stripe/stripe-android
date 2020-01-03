package com.stripe.example.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.example.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_card_payment_methods.*

class CreateCardPaymentMethodActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    private val adapter: PaymentMethodsAdapter = PaymentMethodsAdapter()
    private val stripe: Stripe by lazy {
        Stripe(
            applicationContext,
            PaymentConfiguration.getInstance(this).publishableKey
        )
    }
    private lateinit var snackbarContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_payment_methods)

        snackbarContainer = findViewById(android.R.id.content)

        rv_payment_methods.setHasFixedSize(false)
        rv_payment_methods.layoutManager = LinearLayoutManager(this)
        rv_payment_methods.adapter = adapter

        btn_create_payment_method.setOnClickListener { createPaymentMethod() }
    }

    private fun createPaymentMethod() {
        val paymentMethodCreateParams =
            card_multiline_widget.paymentMethodCreateParams ?: return

        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        val createPaymentMethodObservable = Observable.fromCallable {
            stripe.createPaymentMethodSynchronous(paymentMethodCreateParams)
        }

        compositeDisposable.add(createPaymentMethodObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { onCreatePaymentMethodStart() }
            .doOnComplete { onCreatePaymentMethodCompleted() }
            .subscribe(
                { onCreatedPaymentMethod(it) },
                { showSnackbar(it.localizedMessage.orEmpty()) }
            )
        )
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(snackbarContainer, message, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun onCreatePaymentMethodStart() {
        progress_bar.visibility = View.VISIBLE
        btn_create_payment_method.isEnabled = false
    }

    private fun onCreatePaymentMethodCompleted() {
        progress_bar.visibility = View.INVISIBLE
        btn_create_payment_method.isEnabled = true
    }

    private fun onCreatedPaymentMethod(paymentMethod: PaymentMethod?) {
        if (paymentMethod != null) {
            adapter.paymentMethods.add(0, paymentMethod)
            adapter.notifyItemInserted(0)
        } else {
            showSnackbar("Created null PaymentMethod")
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    private class PaymentMethodsAdapter :
        RecyclerView.Adapter<PaymentMethodsAdapter.PaymentMethodViewHolder>() {
        internal val paymentMethods: MutableList<PaymentMethod> = mutableListOf()

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.payment_method_item, parent, false)
            return PaymentMethodViewHolder(itemView)
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

        internal class PaymentMethodViewHolder internal constructor(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {
            private val idView: TextView = itemView.findViewById(R.id.pm_id)
            private val brandView: TextView = itemView.findViewById(R.id.pm_brand)
            private val last4View: TextView = itemView.findViewById(R.id.pm_last4)

            internal fun setPaymentMethod(paymentMethod: PaymentMethod) {
                val card = paymentMethod.card
                idView.text = paymentMethod.id
                brandView.text = card?.brand.orEmpty()
                last4View.text = card?.last4.orEmpty()
            }
        }
    }
}
