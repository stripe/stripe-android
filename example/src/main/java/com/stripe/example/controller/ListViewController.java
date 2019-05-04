package com.stripe.example.controller;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.stripe.android.model.Token;
import com.stripe.example.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A controller for the {@link ListView} used to display the results.
 */
public class ListViewController {

    @NonNull private final SimpleAdapter mAdapter;
    @NonNull private final List<Map<String, String>> mCardTokens = new ArrayList<>();
    @NonNull private final Resources mResources;

    public ListViewController(@NonNull ListView listView) {
        final Context context = listView.getContext();
        mResources = context.getResources();
        mAdapter = new SimpleAdapter(
                context,
                mCardTokens,
                R.layout.list_item_layout,
                new String[]{"last4", "tokenId"},
                new int[]{R.id.last4, R.id.tokenId});
        listView.setAdapter(mAdapter);
    }

    void addToList(@NonNull Token token) {
        addToList(token.getCard().getLast4(), token.getId());
    }

    void addToList(@NonNull String last4, @NonNull String tokenId) {
        final Map<String, String> map = new HashMap<>();
        map.put("last4", mResources.getString(R.string.endingIn) + " " + last4);
        map.put("tokenId", tokenId);
        mCardTokens.add(map);
        mAdapter.notifyDataSetChanged();
    }
}
