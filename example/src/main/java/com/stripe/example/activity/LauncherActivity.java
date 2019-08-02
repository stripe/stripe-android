package com.stripe.example.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stripe.android.PaymentConfiguration;
import com.stripe.example.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LauncherActivity extends AppCompatActivity {
    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://dashboard.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY =
            "put your key here";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        PaymentConfiguration.init(PUBLISHABLE_KEY);

        final RecyclerView examples = findViewById(R.id.examples);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        examples.setHasFixedSize(true);
        examples.setLayoutManager(linearLayoutManager);
        examples.setAdapter(new ExamplesAdapter(this));
    }

    private static final class ExamplesAdapter
            extends RecyclerView.Adapter<ExamplesAdapter.ExamplesViewHolder> {
        @NonNull private final Activity mActivity;
        @NonNull private final List<Item> mItems;

        private ExamplesAdapter(@NonNull Activity activity) {
            mActivity = activity;
            this.mItems = new ArrayList<>(Arrays.asList(
                    new Item(activity.getString(R.string.launch_payment_intent_example),
                            PaymentIntentActivity.class),
                    new Item(activity.getString(R.string.launch_setup_intent_example),
                            SetupIntentActivity.class),
                    new Item(activity.getString(R.string.payment_auth_example),
                            PaymentAuthActivity.class),
                    new Item(activity.getString(R.string.create_card_tokens),
                            PaymentActivity.class),
                    new Item(activity.getString(R.string.create_card_payment_methods),
                            PaymentMultilineActivity.class),
                    new Item(activity.getString(R.string.create_three_d_secure),
                            RedirectActivity.class),
                    new Item(activity.getString(R.string.launch_customer_session),
                            CustomerSessionActivity.class),
                    new Item(activity.getString(R.string.launch_payment_session),
                            PaymentSessionActivity.class),
                    new Item(activity.getString(R.string.launch_payment_session_from_fragment),
                            FragmentExampesActivity.class),
                    new Item(activity.getString(R.string.launch_pay_with_google),
                            PayWithGoogleActivity.class)
            ));
        }

        @NonNull
        @Override
        public ExamplesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            final View root = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.launcher_item, viewGroup, false);
            return new ExamplesViewHolder(root);
        }

        @Override
        public void onBindViewHolder(@NonNull ExamplesViewHolder examplesViewHolder, int i) {
            ((TextView) examplesViewHolder.itemView).setText(mItems.get(i).mText);
            examplesViewHolder.itemView.setOnClickListener(v ->
                    mActivity.startActivity(new Intent(mActivity, mItems.get(i).mActivityClass)));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        private static final class Item {
            @NonNull private final String mText;
            @NonNull private final Class<?> mActivityClass;

            private Item(@NonNull String text, @NonNull Class<?> activityClass) {
                this.mText = text;
                this.mActivityClass = activityClass;
            }
        }

        private static final class ExamplesViewHolder extends RecyclerView.ViewHolder {
            private ExamplesViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
