package com.stripe.android.stripecardscan.framework

import androidx.test.filters.SmallTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class FetcherTest {

    @Test
    @SmallTest
    @ExperimentalCoroutinesApi
    fun fetchResource_success() = runTest {
        class ResourceFetcherImpl : ResourceFetcher() {
            override val assetFileName: String = "sample_resource.tflite"
            override val modelVersion: String = "sample_resource"
            override val hash: String =
                "0dcf3e387c68dfea8dd72a183f1f765478ebaa4d8544cfc09a16e87a795d8ccf"
            override val hashAlgorithm: String = "SHA-256"
            override val modelClass: String = "sample_class"
            override val modelFrameworkVersion: Int = 2049
        }

        assertEquals(
            expected = FetchedResource(
                modelClass = "sample_class",
                modelFrameworkVersion = 2049,
                modelVersion = "sample_resource",
                modelHash = "0dcf3e387c68dfea8dd72a183f1f765478ebaa4d8544cfc09a16e87a795d8ccf",
                modelHashAlgorithm = "SHA-256",
                assetFileName = "sample_resource.tflite"
            ),
            actual = ResourceFetcherImpl().fetchData(forImmediateUse = false, isOptional = false)
        )
    }
}
