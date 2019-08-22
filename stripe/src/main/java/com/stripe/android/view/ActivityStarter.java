package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.util.Objects;

public abstract class ActivityStarter
        <TargetActivityType extends Activity, ArgsType extends ActivityStarter.Args> {
    @NonNull private final Activity mActivity;
    @Nullable private final Fragment mFragment;
    @NonNull private final Class<TargetActivityType> mTargetClass;
    @NonNull private final ArgsType mDefaultArgs;
    @NonNull private final int mRequestCode;

    ActivityStarter(@NonNull Activity activity,
                    @NonNull Class<TargetActivityType> targetClass,
                    @NonNull ArgsType args,
                    int requestCode) {
        mActivity = activity;
        mFragment = null;
        mTargetClass = targetClass;
        mDefaultArgs = args;
        mRequestCode = requestCode;
    }

    ActivityStarter(@NonNull Fragment fragment,
                    @NonNull Class<TargetActivityType> targetClass,
                    @NonNull ArgsType args,
                    int requestCode) {
        mActivity = fragment.requireActivity();
        mFragment = fragment;
        mTargetClass = targetClass;
        mDefaultArgs = args;
        mRequestCode = requestCode;
    }

    public final void startForResult() {
        startForResult(mDefaultArgs);
    }

    public final void startForResult(@NonNull ArgsType args) {
        final Intent intent = newIntent()
                .putExtra(Args.EXTRA, args);

        if (mFragment != null) {
            Objects.requireNonNull(mFragment).startActivityForResult(intent, mRequestCode);
        } else {
            mActivity.startActivityForResult(intent, mRequestCode);
        }
    }

    @NonNull
    final Intent newIntent() {
        return new Intent(mActivity, mTargetClass);
    }

    public interface Args extends Parcelable {
        String EXTRA = "EXTRA_ARGS";
    }
}
