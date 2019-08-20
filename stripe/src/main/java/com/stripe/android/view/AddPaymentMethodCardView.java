package com.stripe.android.view;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;

/**
 * View for adding a payment method of type {@link PaymentMethod.Type#Card}.
 *
 * See {@link AddPaymentMethodActivity} for usage.
 */
final class AddPaymentMethodCardView extends AddPaymentMethodView {
    @NonNull private final CardMultilineWidget mCardMultilineWidget;

    @NonNull
    static AddPaymentMethodCardView create(@NonNull Context context,
                                                  boolean shouldShowPostalCode) {
        return new AddPaymentMethodCardView(context, null, 0, shouldShowPostalCode);
    }

    public AddPaymentMethodCardView(@NonNull Context context) {
        this(context, null);
    }

    public AddPaymentMethodCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AddPaymentMethodCardView(@NonNull Context context, @Nullable AttributeSet attrs,
                                    int defStyleAttr) {
        this(context, attrs, defStyleAttr, false);
    }

    private AddPaymentMethodCardView(@NonNull Context context, @Nullable AttributeSet attrs,
                                     int defStyleAttr, boolean shouldShowPostalCode) {
        super(context, attrs, defStyleAttr);
        inflate(getContext(), R.layout.add_payment_method_card_layout, this);
        mCardMultilineWidget = findViewById(R.id.add_source_card_entry_widget);
        mCardMultilineWidget.setShouldShowPostalCode(shouldShowPostalCode);
        initEnterListeners();
    }

    private void initEnterListeners() {
        final AddPaymentMethodActivity activity = (AddPaymentMethodActivity) getContext();

        final TextView.OnEditorActionListener listener =
                new AddPaymentMethodCardView.OnEditorActionListenerImpl(
                        activity,
                        this,
                        (InputMethodManager) activity
                                .getSystemService(Activity.INPUT_METHOD_SERVICE)
                );

        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_card_number_ml))
                .setOnEditorActionListener(listener);
        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_expiry_ml))
                .setOnEditorActionListener(listener);
        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_cvc_ml))
                .setOnEditorActionListener(listener);
        ((TextView) mCardMultilineWidget.findViewById(R.id.et_add_source_postal_ml))
                .setOnEditorActionListener(listener);
    }

    @Nullable
    @Override
    public PaymentMethodCreateParams getCreateParams() {
        return mCardMultilineWidget.getPaymentMethodCreateParams();
    }

    @Override
    public void setCommunicatingProgress(boolean communicating) {
        mCardMultilineWidget.setEnabled(!communicating);
    }

    static final class OnEditorActionListenerImpl
            implements TextView.OnEditorActionListener {
        @NonNull private final AddPaymentMethodActivity mActivity;
        @NonNull private final AddPaymentMethodCardView mAddPaymentMethodCardView;
        @NonNull private final InputMethodManager mInputMethodManager;

        OnEditorActionListenerImpl(@NonNull AddPaymentMethodActivity activity,
                                   @NonNull AddPaymentMethodCardView addPaymentMethodCardView,
                                   @NonNull InputMethodManager inputMethodManager) {
            mActivity = activity;
            mAddPaymentMethodCardView = addPaymentMethodCardView;
            mInputMethodManager = inputMethodManager;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mAddPaymentMethodCardView.getCreateParams() != null) {
                    mInputMethodManager.hideSoftInputFromWindow(mActivity.getWindowToken(), 0);
                }
                mActivity.onActionSave();
                return true;
            }
            return false;
        }
    }
}
