package com.stripe.android;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.stripe.android.model.Source;
import com.stripe.android.view.CardMultilineWidget;

public class AddSourceActivity extends AppCompatActivity {

    private static final String EXTRA_SOURCE_TYPE = "source_type";

    FrameLayout mFrameLayout;

    private @Source.SourceType String mSourceType;

    public static Intent newIntent(Context context) {
        return new Intent(context, AddSourceActivity.class)
                .putExtra(EXTRA_SOURCE_TYPE, Source.CARD);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_source);

        mSourceType = Source.asSourceType(getIntent().getStringExtra(EXTRA_SOURCE_TYPE));
        if (!Source.CARD.equals(mSourceType)) {
            finish();
        }

        final CardMultilineWidget cardMultilineWidget = new CardMultilineWidget(this);
        mFrameLayout = (FrameLayout) findViewById(R.id.add_source_primary);
        mFrameLayout.addView(cardMultilineWidget);

        Button addSourceButton = (Button) findViewById(R.id.btn_add_source);
        addSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardMultilineWidget.validateAllFields();
            }
        });
        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            setTitle("Add Card");
        }
        cardMultilineWidget.requestFocus();
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}
