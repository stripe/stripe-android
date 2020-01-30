package com.stripe.example.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import com.stripe.android.view.CardValidCallback
import com.stripe.example.R
import kotlinx.android.synthetic.main.card_token_activity.*

class CreateCardTokenActivity : AppCompatActivity() {

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(findViewById(android.R.id.content))
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.card_token_activity)

        val adapter = Adapter(this)
        tokens_list.adapter = adapter
        tokens_list.layoutManager = LinearLayoutManager(this)
            .apply {
                orientation = LinearLayoutManager.VERTICAL
            }

        val viewModel = ViewModelProvider(
            this
        )[CreateCardTokenViewModel::class.java]

        create_token_button.setOnClickListener {
            BackgroundTaskTracker.onStart()

            val card = card_input_widget.card

            if (card != null) {
                onRequestStart()
                viewModel.createCardToken(card).observe(this, Observer {
                    onRequestEnd()
                    adapter.addToken(it)
                })
            } else {
                snackbarController.show(getString(R.string.invalid_card_details))
            }
        }

        card_input_widget.setCardValidCallback(object : CardValidCallback {
            override fun onInputChanged(
                isValid: Boolean,
                invalidFields: Set<CardValidCallback.Fields>
            ) {
                // added as an example - no-op
            }
        })

        card_input_widget.requestFocus()
    }

    private fun onRequestStart() {
        progress_bar.visibility = View.VISIBLE
        create_token_button.isEnabled = false
    }

    private fun onRequestEnd() {
        progress_bar.visibility = View.INVISIBLE
        create_token_button.isEnabled = true
    }

    internal class Adapter(
        private val activity: Activity
    ) : RecyclerView.Adapter<Adapter.TokenViewHolder>() {
        private val tokens: MutableList<Token> = mutableListOf()

        override fun getItemCount(): Int {
            return tokens.size
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): TokenViewHolder {
            val root = activity.layoutInflater
                .inflate(R.layout.token_item, viewGroup, false)
            return TokenViewHolder(root)
        }

        override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
            holder.update(tokens[position])
        }

        internal fun addToken(token: Token) {
            tokens.add(0, token)
            notifyItemInserted(0)
        }

        internal class TokenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val last4TextView: TextView = itemView.findViewById(R.id.last4)
            private val tokenIdTextView: TextView = itemView.findViewById(R.id.tokenId)

            fun update(token: Token) {
                last4TextView.text = token.card?.last4
                tokenIdTextView.text = token.id
            }
        }
    }

    internal class CreateCardTokenViewModel(
        application: Application
    ) : AndroidViewModel(application) {
        private val context = application.applicationContext
        private val stripe = Stripe(
            context,
            PaymentConfiguration.getInstance(context).publishableKey
        )

        fun createCardToken(card: Card): LiveData<Token> {
            val data = MutableLiveData<Token>()

            stripe.createCardToken(card, callback = object : ApiResultCallback<Token> {
                override fun onSuccess(result: Token) {
                    BackgroundTaskTracker.onStop()
                    data.value = result
                }

                override fun onError(e: Exception) {
                    BackgroundTaskTracker.onStop()
                    Log.e("StripeExample", "Error while creating card token", e)
                }
            })

            return data
        }
    }
}
