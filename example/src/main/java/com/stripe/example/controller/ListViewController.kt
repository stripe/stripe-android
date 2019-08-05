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

    private val mAdapter: SimpleAdapter
    private val mCardTokens = ArrayList<Map<String, String>>()
    private val mResources: Resources

    init {
        val context = listView.context
        mResources = context.resources
        mAdapter = SimpleAdapter(
            context,
            mCardTokens,
            R.layout.list_item_layout,
            arrayOf("last4", "tokenId"),
            intArrayOf(R.id.last4, R.id.tokenId))
        listView.adapter = mAdapter
    }

    internal fun addToList(token: Token) {
        addToList(token.card!!.last4!!, token.id)
    }

    internal fun addToList(last4: String, tokenId: String) {
        val map = hashMapOf(
            "last4" to mResources.getString(R.string.endingIn) + " " + last4,
            "tokenId" to tokenId
        )
        mCardTokens.add(map)
        mAdapter.notifyDataSetChanged()
    }
}
