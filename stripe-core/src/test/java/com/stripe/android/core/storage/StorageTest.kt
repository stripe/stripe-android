package com.stripe.android.core.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PURPOSE_TEST = "test"

@RunWith(RobolectricTestRunner::class)
class StorageTest {
    private val testContext = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @SmallTest
    fun storeAndRetrieveString() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, "test_string"))
        assertEquals("test_string", storage.getString(key, "wrong"))
    }

    @Test
    @SmallTest
    fun storeAndRetrieveLong() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, 1L))
        assertEquals(1L, storage.getLong(key, 2L))
    }

    @Test
    @SmallTest
    fun storeAndRetrieveInt() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, 1))
        assertEquals(1, storage.getInt(key, 2))
    }

    @Test
    @SmallTest
    fun storeAndRetrieveFloat() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, 1F))
        assertEquals(1F, storage.getFloat(key, 2F))
    }

    @Test
    @SmallTest
    fun storeAndRetrieveBoolean() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, true))
        assertEquals(true, storage.getBoolean(key, false))
    }

    @Test
    @SmallTest
    fun retrieveMissingString() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertEquals("default", storage.getString(key, "default"))
    }

    @Test
    @SmallTest
    fun retrieveMissingLong() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertEquals(1L, storage.getLong(key, 1L))
    }

    @Test
    @SmallTest
    fun retrieveMissingInt() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertEquals(1, storage.getInt(key, 1))
    }

    @Test
    @SmallTest
    fun retrieveMissingFloat() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertEquals(1F, storage.getFloat(key, 1F))
    }

    @Test
    @SmallTest
    fun retrieveMissingBoolean() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertEquals(true, storage.getBoolean(key, true))
    }

    @Test
    @SmallTest
    fun retrieveWrongTypeString() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, 1L))
        assertEquals("default", storage.getString(key, "default"))
    }

    @Test
    @SmallTest
    fun retrieveWrongTypeLong() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, 1F))
        assertEquals(1L, storage.getLong(key, 1L))
    }

    @Test
    @SmallTest
    fun retrieveWrongTypeInt() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, 1F))
        assertEquals(1, storage.getInt(key, 1))
    }

    @Test
    @SmallTest
    fun retrieveWrongTypeFloat() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, true))
        assertEquals(1F, storage.getFloat(key, 1F))
    }

    @Test
    @SmallTest
    fun retrieveWrongTypeBoolean() {
        val storage = StorageFactory.getStorageInstance(testContext, PURPOSE_TEST)
        val key = "test"
        assertTrue(storage.storeValue(key, "test_value"))
        assertEquals(true, storage.getBoolean(key, true))
    }
}
