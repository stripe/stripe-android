package com.stripe.android.payments.core.injection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class WeakSetInjectorRegistryTest {

    @Before
    fun clearStaticCache() {
        WeakSetInjectorRegistry.staticCacheSet.clear()
    }

    @Test
    fun verifyRegistryRetrievesCorrectObject() {
        val injector1 = TestInjector()
        val keyForInjector1 = WeakSetInjectorRegistry.nextKey()

        val injector2 = TestInjector()
        val keyForInjector2 = WeakSetInjectorRegistry.nextKey()

        WeakSetInjectorRegistry.register(injector1, keyForInjector1)
        WeakSetInjectorRegistry.register(injector2, keyForInjector2)

        assertEquals(injector1, WeakSetInjectorRegistry.retrieve(keyForInjector1))
        assertEquals(injector2, WeakSetInjectorRegistry.retrieve(keyForInjector2))
        assertNotEquals(
            WeakSetInjectorRegistry.retrieve(keyForInjector1),
            WeakSetInjectorRegistry.retrieve(keyForInjector2)
        )
    }

    @Suppress("UNUSED_VALUE")
    @Test
    fun verifyCacheIsClearedOnceWeakReferenceIsDeReferenced() {
        var injector1: Injector? = TestInjector()
        val keyForInjector1 = WeakSetInjectorRegistry.nextKey()
        WeakSetInjectorRegistry.register(injector1!!, keyForInjector1)

        assertNotNull(WeakSetInjectorRegistry.retrieve(keyForInjector1))

        var retry = 10
        // de-reference the injector and hint System.gc to trigger removal
        injector1 = null
        while (WeakSetInjectorRegistry.retrieve(keyForInjector1) != null && retry > 0) {
            System.gc()
            retry--
        }

        if (retry == 0) {
            assertNull(WeakSetInjectorRegistry.retrieve(keyForInjector1))
        }
        // otherwise WeakSetInjectorRegistry.retrieve(keyForInjector1) == null,
        // indicating the entry is already cleared
    }

    // calling whenever() on a mock will end up holding the mock instance, therefore a real
    // Injector instance is needed to ensure System.gc() works correctly.
    internal class TestInjector : Injector {

        var key: Int? = null

        override fun inject(injectable: Injectable) {
            // no - op
        }

        override fun getInjectorKey(): Int? = key

        override fun setInjectorKey(injectorKey: Int) {
            key = injectorKey
        }

    }
}
