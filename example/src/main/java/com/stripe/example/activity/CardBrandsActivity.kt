package com.stripe.example.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.CardBrand
import com.stripe.example.R
import kotlinx.android.synthetic.main.activity_card_brands.*

class CardBrandsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_brands)

        card_brands.setHasFixedSize(true)
        card_brands.layoutManager = LinearLayoutManager(this)
        card_brands.adapter = Adapter(this)
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
                activity.layoutInflater
                    .inflate(R.layout.card_brand_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: CardBrandViewHolder, position: Int) {
            val brand = CardBrand.values()[position]
            holder.nameView.text = brand.displayName
            holder.logoView.setImageDrawable(
                ContextCompat.getDrawable(activity, brand.icon)
            )
        }

        internal class CardBrandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameView: TextView = itemView.findViewById(R.id.brand_name)
            val logoView: ImageView = itemView.findViewById(R.id.brand_logo)
        }
    }
}
