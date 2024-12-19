package com.stripe.android.link.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
 * Adds an injector to the target [InjectableActivityScenario] for the given [Activity].
 */
inline fun <reified A : Activity> InjectableActivityScenario<*>.injectActivity(noinline injector: A.() -> Unit) {
    injectActivity(A::class.java, injector)
}

/**
 * Adds an injector to the target [InjectableActivityScenario] for the given [Fragment].
 */
inline fun <reified F : Fragment> InjectableActivityScenario<*>.injectFragment(noinline injector: F.() -> Unit) {
    injectFragment(F::class.java, injector)
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
class InjectableActivityScenario<T : Activity>(private val activityClass: Class<T>) : AutoCloseable, Closeable {

    val state: Lifecycle.State?
        get() = delegate?.state

    private var delegate: ActivityScenario<T>? = null

    private val activityInjectors = mutableListOf<ActivityInjector<out Activity>>()
    private val fragmentInjectors = mutableListOf<FragmentInjector<out Fragment>>()

    fun launch(startIntent: Intent? = null): InjectableActivityScenario<T> {
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(activityLifecycleObserver)
        delegate = if (startIntent != null) {
            ActivityScenario.launch(startIntent)
        } else {
            ActivityScenario.launch(activityClass)
        }
        return this
    }

    override fun close() {
        delegate?.close()
        ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(activityLifecycleObserver)
    }

    fun moveToState(newState: Lifecycle.State): InjectableActivityScenario<T> {
        val d =
            delegate ?: throw IllegalStateException("Cannot move to state $newState since the activity hasn't been launched.")
        d.moveToState(newState)
        return this
    }

    fun recreate(): InjectableActivityScenario<T> {
        val d = delegate ?: throw IllegalStateException("Cannot recreate the activity since it hasn't been launched.")
        d.recreate()
        return this
    }

    fun onActivity(action: (T) -> Unit): InjectableActivityScenario<T> {
        val d = delegate ?: throw IllegalStateException("Cannot run onActivity since the activity hasn't been launched.")
        d.onActivity(action)
        return this
    }

    fun getResult(): Instrumentation.ActivityResult =
        delegate?.result ?: throw IllegalStateException("Cannot get result since activity hasn't been launched.")

    /**
     * Injects the target Activity using the supplied [injector].
     *
     */
    fun injectActivity(injector: T.() -> Unit) {
        activityInjectors.add(ActivityInjector(activityClass, injector))
    }

    fun <A : Activity> injectActivity(activityClass: Class<A>, injector: A.() -> Unit) {
        activityInjectors.add(ActivityInjector(activityClass, injector))
    }

    fun <F : Fragment> injectFragment(fragmentClass: Class<F>, injector: F.() -> Unit) {
        fragmentInjectors.add(FragmentInjector(fragmentClass, injector))
    }

    fun <F : Fragment> injectFragment(fragment: F, injector: F.() -> Unit) {
        fragmentInjectors.add(
            FragmentInjector(
                fragment::class.java,
                injector
            )
        )
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
                        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
                    }
                    activityInjectors.forEach { if (it.inject(activity)) return }
                }
                Stage.DESTROYED -> {
                    if (activity is FragmentActivity) {
                        activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
                    }
                }
                else -> {
                    // no op
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel as T
    }
}
