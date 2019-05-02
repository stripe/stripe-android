package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.After;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public abstract class BaseViewTest<T extends Activity> {
    @NonNull private Class<T> mClazz;
    @NonNull private final Map<Integer, ActivityController<T>> mActivityControllers =
            new HashMap<>();

    protected BaseViewTest(@NonNull Class<T> clazz) {
        this.mClazz = clazz;
    }

    @NonNull
    protected T createActivity() {
        return createActivity(null);
    }

    @NonNull
    protected T createActivity(@Nullable Intent intent) {
        final ActivityController<T> activityController = Robolectric.buildActivity(mClazz, intent)
                .create().start()
                .postCreate(null).resume().visible();
        final T activity = activityController.get();
        mActivityControllers.put(activity.hashCode(), activityController);
        return activityController.get();
    }

    @NonNull
    protected T createStartedActivity() {
        final ActivityController<T> activityController = Robolectric.buildActivity(mClazz)
                .create().start();
        final T activity = activityController.get();
        mActivityControllers.put(activity.hashCode(), activityController);
        return activity;
    }

    /**
     * Call resume() and visible() on an Activity that was created with
     * {@link #createStartedActivity()}.
     */
    protected void resumeStartedActivity(T activity) {
        final ActivityController<T> activityController =
                mActivityControllers.get(activity.hashCode());
        if (activityController != null) {
            activityController.resume().visible();
        }
    }

    @CallSuper
    @After
    public void tearDown() {
        for (ActivityController<T> activityController : mActivityControllers.values()) {
            activityController.pause().stop().destroy();
        }
    }
}
