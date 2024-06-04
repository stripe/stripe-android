package com.stripe.android.payments.core.injection

import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class WeakMapInjectorRegistryTest {

    @Before
    fun clearStaticCache() {
        WeakMapInjectorRegistry.clear()
    }

    @Test
    fun verifyRegistryRetrievesCorrectObject() {
        val injector1 = TestInjector()
        val keyForInjector1 =
            WeakMapInjectorRegistry.nextKey(requireNotNull(TestInjector::class.simpleName))

        val injector2 = TestInjector()
        val keyForInjector2 =
            WeakMapInjectorRegistry.nextKey(requireNotNull(TestInjector::class.simpleName))

        WeakMapInjectorRegistry.register(injector1, keyForInjector1)
        WeakMapInjectorRegistry.register(injector2, keyForInjector2)

        assertEquals(injector1, WeakMapInjectorRegistry.retrieve(keyForInjector1))
        assertEquals(injector2, WeakMapInjectorRegistry.retrieve(keyForInjector2))
        assertNotEquals(
            WeakMapInjectorRegistry.retrieve(keyForInjector1),
            WeakMapInjectorRegistry.retrieve(keyForInjector2)
        )
    }

    @Suppress("UNUSED_VALUE")
    @Test
    fun verifyCacheIsClearedOnceWeakReferenceIsDeReferenced() {
        var injector1: Injector? = TestInjector()
        val keyForInjector1 =
            WeakMapInjectorRegistry.nextKey(requireNotNull(TestInjector::class.simpleName))
        WeakMapInjectorRegistry.register(injector1!!, keyForInjector1)

        assertNotNull(WeakMapInjectorRegistry.retrieve(keyForInjector1))

        var retry = 10
        // de-reference the injector and hint System.gc to trigger removal
        injector1 = null
        while (WeakMapInjectorRegistry.retrieve(keyForInjector1) != null && retry > 0) {
            System.gc()
            retry--
        }

        if (retry == 0) {
            assertNull(WeakMapInjectorRegistry.retrieve(keyForInjector1))
        }
        // otherwise WeakSetInjectorRegistry.retrieve(keyForInjector1) == null,
        // indicating the entry is already cleared
    }

    // calling whenever() on a mock will end up holding the mock instance, therefore a real
    // Injector instance is needed to ensure System.gc() works correctly.
    internal class TestInjector : Injector {
        override fun inject(injectable: Injectable<*>) {
            // no - op
        }
    }
}
