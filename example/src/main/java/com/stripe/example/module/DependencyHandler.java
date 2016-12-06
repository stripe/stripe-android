package com.stripe.example.module;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.stripe.example.controller.AsyncTaskTokenController;
import com.stripe.example.controller.CardInformationReader;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.IntentServiceTokenController;
import com.stripe.example.controller.ListViewController;
import com.stripe.example.controller.ProgressDialogController;
import com.stripe.example.controller.RxTokenController;

/**
 * A dagger-free simple way to handle dependencies in the Example project.
 */
public class DependencyHandler {

    private static final String PUBLISHABLE_KEY = "pk_test_6pRNASCoBOKtIshFeQd4XMUh";

    private AsyncTaskTokenController mAsyncTaskController;
    private CardInformationReader mCardInformationReader;
    private ErrorDialogHandler mErrorDialogHandler;
    private IntentServiceTokenController mIntentServiceTokenController;
    private ListViewController mListViewController;
    private RxTokenController mRxTokenController;
    private ProgressDialogController mProgresDialogController;


    public DependencyHandler(
            AppCompatActivity activity,
            EditText cardNumberEditText,
            Spinner monthSpinner,
            Spinner yearSpinner,
            EditText cvcEditText,
            Spinner currencySpinner,
            ListView outputListView) {

        mCardInformationReader = new CardInformationReader(
                cardNumberEditText,
                monthSpinner,
                yearSpinner,
                cvcEditText,
                currencySpinner);

        mProgresDialogController =
                new ProgressDialogController(activity.getSupportFragmentManager());

        mListViewController = new ListViewController(outputListView);

        mErrorDialogHandler = new ErrorDialogHandler(activity.getSupportFragmentManager());
    }

    @NonNull
    public AsyncTaskTokenController getAsyncTaskTokenController(Button button) {
        if (mAsyncTaskController == null) {
            mAsyncTaskController = new AsyncTaskTokenController(
                    button,
                    mCardInformationReader,
                    mErrorDialogHandler,
                    mListViewController,
                    mProgresDialogController,
                    PUBLISHABLE_KEY);
        }
        return mAsyncTaskController;
    }

    @NonNull
    public IntentServiceTokenController getIntentServiceTokenController(
            AppCompatActivity appCompatActivity,
            Button button) {
        if (mIntentServiceTokenController == null) {
            mIntentServiceTokenController = new IntentServiceTokenController(
                    appCompatActivity,
                    button,
                    mCardInformationReader,
                    mErrorDialogHandler,
                    mListViewController,
                    mProgresDialogController,
                    PUBLISHABLE_KEY);
        }
        return mIntentServiceTokenController;
    }

    @NonNull
    public RxTokenController getRxTokenController(Button button) {
        if (mRxTokenController == null) {
            mRxTokenController = new RxTokenController(button,
                    mCardInformationReader,
                    mErrorDialogHandler,
                    mListViewController,
                    mProgresDialogController,
                    PUBLISHABLE_KEY);
        }
        return mRxTokenController;
    }

    public void clearReferences() {
        mAsyncTaskController = null;
        mRxTokenController = null;
        mIntentServiceTokenController = null;
    }
}
