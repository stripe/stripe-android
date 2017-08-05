package com.stripe.example.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.stripe.example.R;
import com.stripe.example.adapter.MaskedCardAdapter;

public class MaskedListActivity extends AppCompatActivity {

    private MaskedCardAdapter mMaskedCardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masked_list);

        RecyclerView recyclerView = findViewById(R.id.card_list);
        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        mMaskedCardAdapter = new MaskedCardAdapter();
        recyclerView.setAdapter(mMaskedCardAdapter);

        Button refreshButton = findViewById(R.id.load_items);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMaskedCardAdapter.load();
            }
        });
    }
}
