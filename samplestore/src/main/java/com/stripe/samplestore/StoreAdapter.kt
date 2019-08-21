package com.stripe.samplestore

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView

import java.util.Currency

class StoreAdapter internal constructor(
    activity: StoreActivity,
    private val priceMultiplier: Float
) : RecyclerView.Adapter<StoreAdapter.ViewHolder>() {

    // Storing an activity here only so we can launch for result
    private val activity: Activity
    private val currency: Currency

    private val quantityOrdered: IntArray
    private var totalOrdered: Int = 0
    private val totalItemsChangedListener: TotalItemsChangedListener

    class ViewHolder(
        pollingLayout: View,
        private val currency: Currency,
        adapter: StoreAdapter
    ) : RecyclerView.ViewHolder(pollingLayout) {
        private val emojiTextView: TextView = pollingLayout.findViewById(R.id.tv_emoji)
        private val priceTextView: TextView = pollingLayout.findViewById(R.id.tv_price)
        private val quantityTextView: TextView = pollingLayout.findViewById(R.id.tv_quantity)
        private val addButton: ImageButton = pollingLayout.findViewById(R.id.tv_plus)
        private val removeButton: ImageButton = pollingLayout.findViewById(R.id.tv_minus)

        private var mPosition: Int = 0

        init {
            addButton.setOnClickListener { adapter.bumpItemQuantity(mPosition, true) }
            removeButton.setOnClickListener { adapter.bumpItemQuantity(mPosition, false) }
        }

        fun setHidden(hidden: Boolean) {
            val visibility = if (hidden) View.INVISIBLE else View.VISIBLE
            emojiTextView.visibility = visibility
            priceTextView.visibility = visibility
            quantityTextView.visibility = visibility
            addButton.visibility = visibility
            removeButton.visibility = visibility
        }

        fun setEmoji(emojiUnicode: Int) {
            emojiTextView.text = StoreUtils.getEmojiByUnicode(emojiUnicode)
        }

        fun setPrice(price: Int) {
            priceTextView.text = StoreUtils.getPriceString(price.toLong(), currency)
        }

        fun setQuantity(quantity: Int) {
            quantityTextView.text = quantity.toString()
        }

        fun setPosition(position: Int) {
            mPosition = position
        }
    }

    init {
        this.activity = activity
        totalItemsChangedListener = activity
        // Note: our sample backend assumes USD as currency. This code would be
        // otherwise functional if you switched that assumption on the backend and passed
        // currency code as a parameter.
        currency = Currency.getInstance(Settings.CURRENCY)
        quantityOrdered = IntArray(EMOJI_CLOTHES.size)
    }

    private fun bumpItemQuantity(index: Int, increase: Boolean) {
        if (index >= 0 && index < quantityOrdered.size) {
            if (increase) {
                quantityOrdered[index]++
                totalOrdered++
                totalItemsChangedListener.onTotalItemsChanged(totalOrdered)
            } else if (quantityOrdered[index] > 0) {
                quantityOrdered[index]--
                totalOrdered--
                totalItemsChangedListener.onTotalItemsChanged(totalOrdered)
            }
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == EMOJI_CLOTHES.size) {
            holder.setHidden(true)
        } else {
            holder.setHidden(false)
            holder.setEmoji(EMOJI_CLOTHES[position])
            holder.setPrice(getPrice(position))
            holder.setQuantity(quantityOrdered[position])
            holder.position = position
        }
    }

    override fun getItemCount(): Int {
        return EMOJI_CLOTHES.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val pollingView = LayoutInflater.from(parent.context)
            .inflate(R.layout.store_item, parent, false)

        return ViewHolder(pollingView, currency, this)
    }

    internal fun launchPurchaseActivityWithCart() {
        val cart = StoreCart(currency)
        for (i in quantityOrdered.indices) {
            if (quantityOrdered[i] > 0) {
                cart.addStoreLineItem(
                    StoreUtils.getEmojiByUnicode(EMOJI_CLOTHES[i]),
                    quantityOrdered[i],
                    getPrice(i).toLong())
            }
        }

        activity.startActivityForResult(
            PaymentActivity.createIntent(activity, cart),
            StoreActivity.PURCHASE_REQUEST)
    }

    internal fun clearItemSelections() {
        for (i in quantityOrdered.indices) {
            quantityOrdered[i] = 0
        }
        notifyDataSetChanged()
        totalItemsChangedListener.onTotalItemsChanged(0)
    }

    private fun getPrice(position: Int): Int {
        return (EMOJI_PRICES[position] * priceMultiplier).toInt()
    }

    interface TotalItemsChangedListener {
        fun onTotalItemsChanged(totalItems: Int)
    }

    companion object {

        private val EMOJI_CLOTHES = intArrayOf(
            0x1F455, 0x1F456, 0x1F457, 0x1F458, 0x1F459, 0x1F45A, 0x1F45B,
            0x1F45C, 0x1F45D, 0x1F45E, 0x1F45F, 0x1F460, 0x1F461, 0x1F462
        )

        private val EMOJI_PRICES = intArrayOf(
            2000, 4000, 3000, 700, 600, 1000, 2000,
            2500, 800, 3000, 2000, 5000, 5500, 6000
        )
    }
}
