package com.stripe.example.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PossibleBrands
import com.stripe.example.StripeFactory
import com.stripe.example.databinding.CardBrandItemBinding
import com.stripe.example.databinding.CardBrandsActivityBinding

class CardBrandsActivity : AppCompatActivity() {
    private val viewBinding: CardBrandsActivityBinding by lazy {
        CardBrandsActivityBinding.inflate(layoutInflater)
    }

    private val stripe: Stripe by lazy {
        StripeFactory(this).create()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.retrieveCardBrands.setOnClickListener {
            val cardNumber = viewBinding.cardBrand.text.toString()
            stripe.retrievePossibleBrands(
                cardNumber,
                object : ApiResultCallback<PossibleBrands> {
                    override fun onSuccess(result: PossibleBrands) {
                        viewBinding.possibleCardBrands.text = "Possible Brands: " +
                            result.brands.toString()
                    }

                    override fun onError(e: Exception) {
                        viewBinding.possibleCardBrands.text = e.toString()
                    }
                }
            )
        }

        viewBinding.cardBrands.setHasFixedSize(true)
        viewBinding.cardBrands.layoutManager = LinearLayoutManager(this)
        viewBinding.cardBrands.adapter = Adapter(this)
    }

    internal class Adapter(
        private val activity: Activity
    ) : RecyclerView.Adapter<Adapter.CardBrandViewHolder>() {
        init {
            setHasStableIds(true)
        }

        override fun getItemCount(): Int {
            return CardBrand.values().size
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): CardBrandViewHolder {
            return CardBrandViewHolder(
                CardBrandItemBinding.inflate(
                    activity.layoutInflater,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: CardBrandViewHolder, position: Int) {
            val brand = CardBrand.values()[position]
            holder.viewBinding.brandName.text = brand.displayName
            holder.viewBinding.brandLogo.setImageDrawable(
                ContextCompat.getDrawable(activity, brand.icon)
            )
        }

        internal class CardBrandViewHolder(
            internal val viewBinding: CardBrandItemBinding
        ) : RecyclerView.ViewHolder(viewBinding.root)
    }
}
