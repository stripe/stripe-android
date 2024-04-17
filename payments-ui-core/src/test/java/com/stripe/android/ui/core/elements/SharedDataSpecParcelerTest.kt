package com.stripe.android.ui.core.elements

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.parcelize.Parcelize
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedDataSpecParcelerTest {
    @Test
    fun `parceled specs result in the same specs`() {
        val inputStream = SharedDataSpecParcelerTest::class.java.classLoader!!.getResourceAsStream("lpms.json")
        val specsString = inputStream.bufferedReader().use { it.readText() }
        val originalSpecs = LpmSerializer.deserializeList(specsString).getOrThrow()
        assertThat(originalSpecs).isNotEmpty()

        val example = ExampleWithList(originalSpecs)
        val parcelizedExample = example.testParcel()

        assertThat(originalSpecs.size).isEqualTo(parcelizedExample.specs.size)
        originalSpecs.indices.forEach { index ->
            assertThat(originalSpecs[index]).isEqualTo(parcelizedExample.specs[index])
        }
        assertThat(originalSpecs).isEqualTo(parcelizedExample.specs)
    }

    @Parcelize
    data class ExampleWithList(
        val specs: List<SharedDataSpec>
    ) : Parcelable
}

private inline fun <reified R : Parcelable> R.testParcel(): R {
    val bytes = marshallParcelable(this)
    return unmarshallParcelable(bytes)
}

private inline fun <reified R : Parcelable> marshallParcelable(parcelable: R): ByteArray {
    val bundle = Bundle().apply { putParcelable(R::class.java.name, parcelable) }
    return marshall(bundle)
}

private fun marshall(bundle: Bundle): ByteArray =
    Parcel.obtain().use {
        it.writeBundle(bundle)
        it.marshall()
    }

private inline fun <reified R : Parcelable> unmarshallParcelable(bytes: ByteArray): R = unmarshall(bytes)
    .readBundle()!!
    .run {
        classLoader = R::class.java.classLoader
        getParcelable(R::class.java.name)!!
    }

private fun unmarshall(bytes: ByteArray): Parcel =
    Parcel.obtain().apply {
        unmarshall(bytes, 0, bytes.size)
        setDataPosition(0)
    }

private fun <T> Parcel.use(block: (Parcel) -> T): T = try {
    block(this)
} finally {
    this.recycle()
}
