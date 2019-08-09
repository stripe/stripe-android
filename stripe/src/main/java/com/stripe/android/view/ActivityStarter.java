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

    ActivityStarter(@NonNull Activity activity,
                    @NonNull Class<TargetActivityType> targetClass) {
        mActivity = activity;
        mFragment = null;
        mTargetClass = targetClass;
    }

    ActivityStarter(@NonNull Fragment fragment,
                    @NonNull Class<TargetActivityType> targetClass) {
        mActivity = fragment.requireActivity();
        mFragment = fragment;
        mTargetClass = targetClass;
    }

    public final void startForResult(final int requestCode) {
        final Intent intent = newIntent();

        if (mFragment != null) {
            Objects.requireNonNull(mFragment).startActivityForResult(intent, requestCode);
        } else {
            mActivity.startActivityForResult(intent, requestCode);
        }
    }

    public final void startForResult(int requestCode, @NonNull ArgsType args) {
        final Intent intent = newIntent()
                .putExtra(Args.EXTRA, args);

        if (mFragment != null) {
            Objects.requireNonNull(mFragment).startActivityForResult(intent, requestCode);
        } else {
            mActivity.startActivityForResult(intent, requestCode);
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
