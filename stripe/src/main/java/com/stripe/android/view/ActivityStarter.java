package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.util.Objects;

public abstract class ActivityStarter<T> {
    @NonNull private final Activity mActivity;
    @Nullable private final Fragment mFragment;
    @NonNull private final Class<T> mTargetClass;

    public ActivityStarter(@NonNull Activity activity, @NonNull Class<T> targetClass) {
        mActivity = activity;
        mFragment = null;
        mTargetClass = targetClass;
    }

    public ActivityStarter(@NonNull Fragment fragment, @NonNull Class<T> targetClass) {
        mActivity = fragment.requireActivity();
        mFragment = fragment;
        mTargetClass = targetClass;
    }

    public final void startForResult(final int requestCode) {
        startForResult(requestCode, new Bundle());
    }

    public final void startForResult(int requestCode, @NonNull Bundle bundle) {
        final Intent intent = newIntent();
        intent.putExtras(bundle);

        if (mFragment != null) {
            Objects.requireNonNull(mFragment).startActivityForResult(intent, requestCode);
        } else {
            mActivity.startActivityForResult(intent, requestCode);
        }
    }

    @NonNull
    public final Intent newIntent() {
        return new Intent(mActivity, mTargetClass);
    }
}
