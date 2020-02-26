package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.CardBrand
import com.stripe.example.databinding.CardBrandItemBinding
import com.stripe.example.databinding.CardBrandsActivityBinding

class CardBrandsActivity : AppCompatActivity() {
    private val viewBinding: CardBrandsActivityBinding by lazy {
        CardBrandsActivityBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

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
