package com.stripe.example.controller;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;

import com.stripe.example.dialog.ProgressDialogFragment;

import java.lang.ref.WeakReference;

/**
 * Class used to show and hide the progress spinner.
 */
public class ProgressDialogController {

    @NonNull private final Resources mRes;
    @NonNull private final FragmentManager mFragmentManager;
    @Nullable private WeakReference<ProgressDialogFragment> mProgressFragmentRef;

    public ProgressDialogController(@NonNull FragmentManager fragmentManager,
                                    @NonNull Resources res) {
        mFragmentManager = fragmentManager;
        mRes = res;
    }

    public void show(@StringRes int resId) {
        dismiss();
        final ProgressDialogFragment progressDialogFragment =
                ProgressDialogFragment.newInstance(mRes.getString(resId));
        progressDialogFragment.show(mFragmentManager, "progress");
        mProgressFragmentRef = new WeakReference<>(progressDialogFragment);
    }

    public void dismiss() {
        final ProgressDialogFragment progressDialogFragment = getDialogFragment();
        if (progressDialogFragment != null) {
            progressDialogFragment.dismiss();

            if (mProgressFragmentRef != null) {
                mProgressFragmentRef.clear();
                mProgressFragmentRef = null;
            }
        }
    }

    @Nullable
    private ProgressDialogFragment getDialogFragment() {
        return mProgressFragmentRef != null ? mProgressFragmentRef.get() : null;
    }
}
