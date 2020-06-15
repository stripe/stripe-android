package com.stripe.example.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.Card
import com.stripe.android.model.Token
import com.stripe.android.view.CardValidCallback
import com.stripe.example.R
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.CreateCardTokenActivityBinding
import com.stripe.example.databinding.TokenItemBinding

class CreateCardTokenActivity : AppCompatActivity() {
    private val viewBinding: CreateCardTokenActivityBinding by lazy {
        CreateCardTokenActivityBinding.inflate(layoutInflater)
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        val adapter = Adapter(this)
        viewBinding.tokensList.adapter = adapter
        viewBinding.tokensList.layoutManager = LinearLayoutManager(this)
            .apply {
                orientation = LinearLayoutManager.VERTICAL
            }

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[CreateCardTokenViewModel::class.java]

        viewBinding.createTokenButton.setOnClickListener {
            BackgroundTaskTracker.onStart()

            val card = viewBinding.cardInputWidget.card

            if (card != null) {
                onRequestStart()
                viewModel.createCardToken(card).observe(
                    this,
                    Observer {
                        onRequestEnd()
                        adapter.addToken(it)
                    }
                )
            } else {
                snackbarController.show(getString(R.string.invalid_card_details))
            }
        }

        viewBinding.cardInputWidget.setCardValidCallback(object : CardValidCallback {
            override fun onInputChanged(
                isValid: Boolean,
                invalidFields: Set<CardValidCallback.Fields>
            ) {
                // added as an example - no-op
            }
        })

        viewBinding.cardInputWidget.requestFocus()
    }

    private fun onRequestStart() {
        viewBinding.progressBar.visibility = View.VISIBLE
        viewBinding.createTokenButton.isEnabled = false
    }

    private fun onRequestEnd() {
        viewBinding.progressBar.visibility = View.INVISIBLE
        viewBinding.createTokenButton.isEnabled = true
    }

    internal class Adapter(
        private val activity: Activity
    ) : RecyclerView.Adapter<Adapter.TokenViewHolder>() {
        private val tokens: MutableList<Token> = mutableListOf()

        override fun getItemCount(): Int {
            return tokens.size
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): TokenViewHolder {
            return TokenViewHolder(
                TokenItemBinding.inflate(
                    activity.layoutInflater,
                    viewGroup,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
            holder.update(tokens[position])
        }

        internal fun addToken(token: Token) {
            tokens.add(0, token)
            notifyItemInserted(0)
        }

        internal class TokenViewHolder(
            private val viewBinding: TokenItemBinding
        ) : RecyclerView.ViewHolder(viewBinding.root) {
            fun update(token: Token) {
                viewBinding.last4.text = token.card?.last4
                viewBinding.tokenId.text = token.id
            }
        }
    }

    internal class CreateCardTokenViewModel(
        application: Application
    ) : AndroidViewModel(application) {
        private val stripe = StripeFactory(application).create()

        fun createCardToken(card: Card): LiveData<Token> {
            val data = MutableLiveData<Token>()

            stripe.createCardToken(
                card,
                callback = object : ApiResultCallback<Token> {
                    override fun onSuccess(result: Token) {
                        BackgroundTaskTracker.onStop()
                        data.value = result
                    }

                    override fun onError(e: Exception) {
                        BackgroundTaskTracker.onStop()
                        Log.e("StripeExample", "Error while creating card token", e)
                    }
                }
            )

            return data
        }
    }
}
