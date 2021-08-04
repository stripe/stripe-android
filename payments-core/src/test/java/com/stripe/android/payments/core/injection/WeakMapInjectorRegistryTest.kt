package com.stripe.android.payments.core.injection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class WeakMapInjectorRegistryTest {

    @Before
    fun clearStaticCache() {
        WeakMapInjectorRegistry.staticCache.clear()
    }

    @Test
    fun verifyRegistryRetrievesCorrectObject() {
        val injector1 = mock<Injector>()
        val keyForInjector1 = WeakMapInjectorRegistry.nextKey()

        val injector2 = mock<Injector>()
        val keyForInjector2 = WeakMapInjectorRegistry.nextKey()

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
        var injector1: Injector? = mock()
        val keyForInjector1 = WeakMapInjectorRegistry.nextKey()
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
        // otherwise WeakMapInjectorRegistry.retrieve(keyForInjector1) == null,
        // indicating the entry is already cleared
    }
}
