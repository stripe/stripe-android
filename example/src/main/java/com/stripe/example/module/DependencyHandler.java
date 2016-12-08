package com.stripe.example.module;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.stripe.android.model.Card;
import com.stripe.example.controller.AsyncTaskTokenController;
import com.stripe.example.controller.CardInformationReader;
import com.stripe.example.controller.CardValidationController;
import com.stripe.example.controller.MessageDialogHandler;
import com.stripe.example.controller.IntentServiceTokenController;
import com.stripe.example.controller.ListViewController;
import com.stripe.example.controller.ProgressDialogController;
import com.stripe.example.controller.RxTokenController;
import com.stripe.example.view.CreditCardView;

/**
 * A dagger-free simple way to handle dependencies in the Example project. Most of this work would
 * ordinarily be done in a module class.
 */
public class DependencyHandler {

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://manage.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY = "pk_test_6pRNASCoBOKtIshFeQd4XMUh";

    private AsyncTaskTokenController mAsyncTaskController;
    private CardInformationReader mCardInformationReader;
    private MessageDialogHandler mMessageDialogHandler;
    private IntentServiceTokenController mIntentServiceTokenController;
    private ListViewController mListViewController;
    private RxTokenController mRxTokenController;
    private ProgressDialogController mProgresDialogController;
    private CardValidationController mCardValidationController;

    public DependencyHandler(
            AppCompatActivity activity,
            CreditCardView creditCardView,
            TextView validationErrorTextView,
            Spinner currencySpinner,
            ListView outputListView) {

        mCardInformationReader = new CardInformationReader(
                creditCardView,
                currencySpinner);

        mProgresDialogController =
                new ProgressDialogController(activity.getSupportFragmentManager());

        mListViewController = new ListViewController(outputListView);

        mMessageDialogHandler = new MessageDialogHandler(activity.getSupportFragmentManager());

        mCardValidationController = new CardValidationController(creditCardView, validationErrorTextView, mMessageDialogHandler);
    }

    /**
     * Attach a listener that creates a token using the {@link android.os.AsyncTask}-based method.
     * Only gets attached once, unless you call {@link #clearReferences()}.
     *
     * @param button a button that, when clicked, gets a token.
     * @return a reference to the {@link AsyncTaskTokenController}
     */
    @NonNull
    public AsyncTaskTokenController attachAsyncTaskTokenController(Button button) {
        if (mAsyncTaskController == null) {
            mAsyncTaskController = new AsyncTaskTokenController(
                    button,
                    mCardInformationReader,
                    mMessageDialogHandler,
                    mListViewController,
                    mProgresDialogController,
                    PUBLISHABLE_KEY);
        }
        return mAsyncTaskController;
    }

    /**
     * Attach a listener that creates a token using an {@link android.app.IntentService} and the
     * synchronous {@link com.stripe.android.Stripe#createTokenSynchronous(Card, String)} method.
     *
     * Only gets attached once, unless you call {@link #clearReferences()}.
     *
     * @param button a button that, when clicked, gets a token.
     * @return a reference to the {@link IntentServiceTokenController}
     */
    @NonNull
    public IntentServiceTokenController attachIntentServiceTokenController(
            AppCompatActivity appCompatActivity,
            Button button) {
        if (mIntentServiceTokenController == null) {
            mIntentServiceTokenController = new IntentServiceTokenController(
                    appCompatActivity,
                    button,
                    mCardInformationReader,
                    mMessageDialogHandler,
                    mListViewController,
                    mProgresDialogController,
                    PUBLISHABLE_KEY);
        }
        return mIntentServiceTokenController;
    }

    /**
     * Attach a listener that creates a token using a {@link rx.Subscription} and the
     * synchronous {@link com.stripe.android.Stripe#createTokenSynchronous(Card, String)} method.
     *
     * Only gets attached once, unless you call {@link #clearReferences()}.
     *
     * @param button a button that, when clicked, gets a token.
     * @return a reference to the {@link RxTokenController}
     */
    @NonNull
    public RxTokenController attachRxTokenController(Button button) {
        if (mRxTokenController == null) {
            mRxTokenController = new RxTokenController(
                    button,
                    mCardInformationReader,
                    mMessageDialogHandler,
                    mListViewController,
                    mProgresDialogController,
                    PUBLISHABLE_KEY);
        }
        return mRxTokenController;
    }

    /**
     * Clear all the references so that we can start over again.
     */
    public void clearReferences() {
        if (mRxTokenController != null) {
            mRxTokenController.detach();
        }

        if (mIntentServiceTokenController != null) {
            mIntentServiceTokenController.detach();
        }

        mAsyncTaskController = null;
        mRxTokenController = null;
        mIntentServiceTokenController = null;
        mCardValidationController = null;
    }
}
