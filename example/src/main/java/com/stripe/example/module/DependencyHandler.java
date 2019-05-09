package com.stripe.example.module;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ListView;

import com.stripe.android.model.Card;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.controller.AsyncTaskTokenController;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.IntentServiceTokenController;
import com.stripe.example.controller.ListViewController;
import com.stripe.example.controller.ProgressDialogController;
import com.stripe.example.controller.RxTokenController;

/**
 * A dagger-free simple way to handle dependencies in the Example project. Most of this work would
 * ordinarily be done in a module class.
 */
public class DependencyHandler {

    @Nullable private AsyncTaskTokenController mAsyncTaskController;
    @NonNull private final CardInputWidget mCardInputWidget;
    @NonNull private final Context mContext;
    @NonNull private final ProgressDialogController mProgressDialogController;
    @NonNull private final ErrorDialogHandler mErrorDialogHandler;
    @Nullable private IntentServiceTokenController mIntentServiceTokenController;
    @NonNull private final ListViewController mListViewController;
    @Nullable private RxTokenController mRxTokenController;

    public DependencyHandler(
            @NonNull AppCompatActivity activity,
            @NonNull CardInputWidget cardInputWidget,
            @NonNull ListView outputListView) {

        mCardInputWidget = cardInputWidget;
        mContext = activity.getApplicationContext();

        mProgressDialogController = new ProgressDialogController(
                activity.getSupportFragmentManager(),
                activity.getResources()
        );

        mListViewController = new ListViewController(outputListView);

        mErrorDialogHandler = new ErrorDialogHandler(activity);
    }

    /**
     * Attach a listener that creates a token using the {@link android.os.AsyncTask}-based method.
     * Only gets attached once, unless you call {@link #clearReferences()}.
     *
     * @param button a button that, when clicked, gets a token.
     */
    public void attachAsyncTaskTokenController(@NonNull Button button) {
        if (mAsyncTaskController == null) {
            mAsyncTaskController = new AsyncTaskTokenController(
                    button,
                    mCardInputWidget,
                    mContext,
                    mErrorDialogHandler,
                    mListViewController,
                    mProgressDialogController);
        }
    }

    /**
     * Attach a listener that creates a token using an {@link android.app.IntentService} and the
     * synchronous {@link com.stripe.android.Stripe#createTokenSynchronous(Card, String)} method.
     *
     * Only gets attached once, unless you call {@link #clearReferences()}.
     *
     * @param button a button that, when clicked, gets a token.
     */
    public void attachIntentServiceTokenController(
            @NonNull Activity activity,
            @NonNull Button button) {
        if (mIntentServiceTokenController == null) {
            mIntentServiceTokenController = new IntentServiceTokenController(
                    activity,
                    button,
                    mCardInputWidget,
                    mErrorDialogHandler,
                    mListViewController,
                    mProgressDialogController);
        }
    }

    /**
     * Attach a listener that creates a token using a {@link rx.Subscription} and the
     * synchronous {@link com.stripe.android.Stripe#createTokenSynchronous(Card, String)} method.
     *
     * Only gets attached once, unless you call {@link #clearReferences()}.
     *
     * @param button a button that, when clicked, gets a token.
     */
    public void attachRxTokenController(@NonNull Button button) {
        if (mRxTokenController == null) {
            mRxTokenController = new RxTokenController(
                    button,
                    mCardInputWidget,
                    mContext,
                    mErrorDialogHandler,
                    mListViewController,
                    mProgressDialogController);
        }
    }

    /**
     * Clear all the references so that we can start over again.
     */
    public void clearReferences() {

        if (mAsyncTaskController != null) {
            mAsyncTaskController.detach();
        }

        if (mRxTokenController != null) {
            mRxTokenController.detach();
        }

        if (mIntentServiceTokenController != null) {
            mIntentServiceTokenController.detach();
        }

        mAsyncTaskController = null;
        mRxTokenController = null;
        mIntentServiceTokenController = null;
    }
}
