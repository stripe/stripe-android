package com.stripe.android;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.StripeException;

import java.io.IOException;

import org.json.JSONException;

abstract class ApiOperation<ResultType>
        extends AsyncTask<Void, Void, ResultWrapper<ResultType>> {
    @NonNull private final ApiResultCallback<ResultType> mCallback;

    ApiOperation(@NonNull ApiResultCallback<ResultType> callback) {
        mCallback = callback;
    }

    @Override
    protected final ResultWrapper<ResultType> doInBackground(Void... voids) {
        try {
            return ResultWrapper.create(getResult());
        } catch (StripeException | JSONException | IOException e) {
            return ResultWrapper.create(e);
        }
    }

    @Override
    protected final void onPostExecute(@NonNull ResultWrapper<ResultType> resultWrapper) {
        super.onPostExecute(resultWrapper);
        if (resultWrapper.getResult() != null) {
            mCallback.onSuccess(resultWrapper.getResult());
        } else if (resultWrapper.getError() != null) {
            mCallback.onError(resultWrapper.getError());
        } else {
            mCallback.onError(new RuntimeException(
                    "The API operation returned neither a result or exception"));
        }
    }

    @Nullable
    abstract ResultType getResult() throws StripeException, JSONException, IOException;
}
