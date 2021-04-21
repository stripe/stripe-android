package com.stripe.example.activity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.createCardToken
import com.stripe.android.model.CardParams
import com.stripe.android.model.Token
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

        val viewModel: CreateCardTokenViewModel by viewModels()

        viewBinding.createTokenButton.setOnClickListener {
            BackgroundTaskTracker.onStart()

            viewBinding.cardInputWidget.cardParams?.let { cardParams ->
                onRequestStart()
                viewModel.createCardToken(cardParams).observe(
                    this,
                    {
                        onRequestEnd()
                        adapter.addToken(it)
                    }
                )
            } ?: snackbarController.show(getString(R.string.invalid_card_details))
        }

        viewBinding.cardInputWidget.setCardValidCallback { isValid, invalidFields ->
            // added as an example - no-op
        }

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

        fun createCardToken(cardParams: CardParams): LiveData<Token> = liveData {
            runCatching {
                stripe.createCardToken(cardParams)
            }.also {
                BackgroundTaskTracker.onStop()
            }.fold(
                onSuccess = {
                    emit(it)
                },
                onFailure = {
                    Log.e("StripeExample", "Error while creating card token", it)
                }
            )
        }
    }
}
