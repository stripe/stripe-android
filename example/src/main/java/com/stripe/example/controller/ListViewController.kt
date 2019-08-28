package com.stripe.example.controller

import android.content.res.Resources
import android.widget.ListView
import android.widget.SimpleAdapter
import com.stripe.android.model.Token
import com.stripe.example.R
import java.util.ArrayList

/**
 * A controller for the [ListView] used to display the results.
 */
class ListViewController(listView: ListView) {

    private val adapter: SimpleAdapter
    private val cardTokens = ArrayList<Map<String, String>>()
    private val resources: Resources

    init {
        val context = listView.context
        resources = context.resources
        adapter = SimpleAdapter(
            context,
            cardTokens,
            R.layout.list_item_layout,
            arrayOf("last4", "tokenId"),
            intArrayOf(R.id.last4, R.id.tokenId))
        listView.adapter = adapter
    }

    internal fun addToList(token: Token) {
        addToList(token.card!!.last4!!, token.id)
    }

    internal fun addToList(last4: String, tokenId: String) {
        val map = hashMapOf(
            "last4" to resources.getString(R.string.endingIn) + " " + last4,
            "tokenId" to tokenId
        )
        cardTokens.add(map)
        adapter.notifyDataSetChanged()
    }
}
