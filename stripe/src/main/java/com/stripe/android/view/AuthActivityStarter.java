package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;

public interface AuthActivityStarter<StartDataType> {
    void start(@NonNull StartDataType data);

    /**
     * A representation of an object (i.e. Activity or Fragment) that can start an activity.
     */
    final class Host {
        @NonNull private final WeakReference<Activity> mActivityRef;
        @Nullable private final WeakReference<Fragment> mFragmentRef;

        @NonNull
        public static Host create(@NonNull Fragment fragment) {
            return new Host(fragment.requireActivity(), fragment);
        }

        public static Host create(@NonNull Activity activity) {
            return new Host(activity, null);
        }

        private Host(@NonNull Activity activity, @Nullable Fragment fragment) {
            this.mActivityRef = new WeakReference<>(activity);
            this.mFragmentRef = fragment != null ? new WeakReference<>(fragment) : null;
        }

        @Nullable
        public Activity getActivity() {
            return mActivityRef.get();
        }

        public void startActivityForResult(@NonNull Class target,
                                           @NonNull Bundle extras,
                                           int requestCode) {
            final Activity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }

            final Intent intent = new Intent(activity, target).putExtras(extras);

            if (mFragmentRef != null) {
                final Fragment fragment = mFragmentRef.get();
                if (fragment != null) {
                    fragment.startActivityForResult(intent, requestCode);
                }
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
        }
    }
}
