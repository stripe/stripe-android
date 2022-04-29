package com.stripe.android.financialconnections.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.test.core.app.ActivityScenario
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.Closeable

/**
 * Creates an [InjectableActivityScenario] that uses the supplied injections.
 *
 * ```
 * injectableActivityScenario<MyActivity> {
 *   injectActivity { // this: MyActivity
 *     dependency1 = myMockDependency1
 *     dependency2 = myMockDependency2
 *   }
 *   injectFragment<MyFragment> { // this: MyFragment
 *     dependency1 = myMockDependency1
 *   }
 * }
 * ```
 */
inline fun <reified T : Activity> injectableActivityScenario(injector: InjectableActivityScenario<T>.() -> Unit) =
    InjectableActivityScenario(T::class.java).apply {
        this.injector()
    }

/**
 * InjectableActivityScenario is an extension and wrapper of [ActivityScenario] which allows you to easily inject
 * any [Activity]s and [Fragment]s that need to be loaded from the launch Activity.
 *
 * Based on https://gist.github.com/rharter/5939bb73207aeb6f24c8522ecf4c9d72
 *
 * ```
 * val activityScenario = InjectableActivityScenario(MyActivity::class.java)
 * activityScenario.injectActivity { // this: MyActivity
 *   dependency1 = myMockDependency1
 *   dependency2 = myMockDependency2
 * }
 * activityScenario.injectFragment(MyFragment::class.java) { // this: MyFragment
 *   dependency1 = myMockDependency1
 * }
 * ```
 */
class InjectableActivityScenario<T : Activity>(
    private val activityClass: Class<T>
) : AutoCloseable, Closeable {

    private var delegate: ActivityScenario<T>? = null

    private val activityInjectors = mutableListOf<ActivityInjector<out Activity>>()
    private val fragmentInjectors = mutableListOf<FragmentInjector<out Fragment>>()

    fun launch(startIntent: Intent? = null): InjectableActivityScenario<T> {
        ActivityLifecycleMonitorRegistry.getInstance()
            .addLifecycleCallback(activityLifecycleObserver)
        delegate = if (startIntent != null) {
            ActivityScenario.launch(startIntent)
        } else {
            ActivityScenario.launch(activityClass)
        }
        return this
    }

    override fun close() {
        delegate?.close()
        ActivityLifecycleMonitorRegistry.getInstance()
            .removeLifecycleCallback(activityLifecycleObserver)
    }

    fun onActivity(action: (T) -> Unit): InjectableActivityScenario<T> {
        val d = delegate ?: throw IllegalStateException(
            "Cannot run onActivity since the activity hasn't been launched."
        )
        d.onActivity(action)
        return this
    }

    fun getResult(): Instrumentation.ActivityResult =
        delegate?.result ?: throw IllegalStateException(
            "Cannot get result since activity hasn't been launched."
        )

    /**
     * Injects the target Activity using the supplied [injector].
     *
     */
    fun injectActivity(injector: T.() -> Unit) {
        activityInjectors.add(ActivityInjector(activityClass, injector))
    }

    private class ActivityInjector<A : Activity>(
        private val activityClass: Class<A>,
        private val injector: A.() -> Unit
    ) {
        fun inject(activity: Activity?): Boolean {
            if (activityClass.isInstance(activity)) {
                activityClass.cast(activity)!!.injector()
                return true
            }
            return false
        }
    }

    private class FragmentInjector<F : Fragment>(
        private val fragmentClass: Class<F>,
        private val injection: F.() -> Unit
    ) {
        fun inject(fragment: Fragment?): Boolean {
            if (fragmentClass.isInstance(fragment)) {
                fragmentClass.cast(fragment)!!.injection()
                return true
            }
            return false
        }
    }

    private val fragmentCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentPreAttached(fm: FragmentManager, f: Fragment, context: Context) {
            fragmentInjectors.forEach {
                if (it.inject(f)) return
            }
        }
    }

    private val activityLifecycleObserver = object : ActivityLifecycleCallback {
        override fun onActivityLifecycleChanged(activity: Activity?, stage: Stage?) {
            when (stage) {
                Stage.PRE_ON_CREATE -> {
                    if (activity is FragmentActivity) {
                        activity.supportFragmentManager
                            .registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
                    }
                    activityInjectors.forEach { if (it.inject(activity)) return }
                }
                Stage.DESTROYED -> {
                    if (activity is FragmentActivity) {
                        activity.supportFragmentManager
                            .unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
                    }
                }
                else -> {
                    // no op
                }
            }
        }
    }
}
