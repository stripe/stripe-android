package com.stripe.example.module

import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.ListView
import com.stripe.android.view.CardInputWidget
import com.stripe.example.controller.AsyncTaskTokenController
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.controller.IntentServiceTokenController
import com.stripe.example.controller.ListViewController
import com.stripe.example.controller.ProgressDialogController
import com.stripe.example.controller.RxTokenController

/**
 * A dagger-free simple way to handle dependencies in the Example project. Most of this work would
 * ordinarily be done in a module class.
 */
class DependencyHandler(
    activity: AppCompatActivity,
    private val mCardInputWidget: CardInputWidget,
    outputListView: ListView,
    private val mPublishableKey: String
) {

    private var mAsyncTaskController: AsyncTaskTokenController? = null
    private val mContext: Context = activity.applicationContext
    private val mProgressDialogController: ProgressDialogController = ProgressDialogController(
        activity.supportFragmentManager,
        activity.resources
    )
    private val mErrorDialogHandler: ErrorDialogHandler = ErrorDialogHandler(activity)
    private var mIntentServiceTokenController: IntentServiceTokenController? = null
    private val mListViewController: ListViewController = ListViewController(outputListView)
    private var mRxTokenController: RxTokenController? = null

    /**
     * Attach a listener that creates a token using the [android.os.AsyncTask]-based method.
     * Only gets attached once, unless you call [.clearReferences].
     *
     * @param button a button that, when clicked, gets a token.
     */
    fun attachAsyncTaskTokenController(button: Button) {
        if (mAsyncTaskController == null) {
            mAsyncTaskController = AsyncTaskTokenController(
                button,
                mCardInputWidget,
                mContext,
                mErrorDialogHandler,
                mListViewController,
                mProgressDialogController,
                mPublishableKey)
        }
    }

    /**
     * Attach a listener that creates a token using an [android.app.IntentService] and the
     * synchronous [com.stripe.android.Stripe.createTokenSynchronous] method.
     *
     * Only gets attached once, unless you call [.clearReferences].
     *
     * @param button a button that, when clicked, gets a token.
     */
    fun attachIntentServiceTokenController(activity: Activity, button: Button) {
        if (mIntentServiceTokenController == null) {
            mIntentServiceTokenController = IntentServiceTokenController(
                activity,
                button,
                mCardInputWidget,
                mErrorDialogHandler,
                mListViewController,
                mProgressDialogController)
        }
    }

    /**
     * Attach a listener that creates a token using a [rx.Subscription] and the
     * synchronous [com.stripe.android.Stripe.createTokenSynchronous] method.
     *
     * Only gets attached once, unless you call [.clearReferences].
     *
     * @param button a button that, when clicked, gets a token.
     */
    fun attachRxTokenController(button: Button) {
        if (mRxTokenController == null) {
            mRxTokenController = RxTokenController(
                button,
                mCardInputWidget,
                mContext,
                mErrorDialogHandler,
                mListViewController,
                mProgressDialogController,
                mPublishableKey)
        }
    }

    /**
     * Clear all the references so that we can start over again.
     */
    fun clearReferences() {

        if (mAsyncTaskController != null) {
            mAsyncTaskController!!.detach()
        }

        if (mRxTokenController != null) {
            mRxTokenController!!.detach()
        }

        if (mIntentServiceTokenController != null) {
            mIntentServiceTokenController!!.detach()
        }

        mAsyncTaskController = null
        mRxTokenController = null
        mIntentServiceTokenController = null
    }
}
