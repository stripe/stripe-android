package com.stripe.android.identity.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.mlcore.impl.InterpreterWrapperImpl
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultIdentityModelFetcherTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `cached model validation closes its temporary interpreter`() = runTest {
        val file = temporaryFolder.newFile("model.tflite")
        val modelUrl = "https://example.com/model.tflite"
        val identityIO = mock<IdentityIO>()
        whenever(identityIO.createTFLiteFile(modelUrl)).thenReturn(file)
        val fetcher = DefaultIdentityModelFetcher(mock(), identityIO, mock())

        mockConstruction(InterpreterWrapperImpl::class.java).use { interpreters ->
            assertThat(fetcher.fetchIdentityModel(modelUrl)).isEqualTo(file)
            verify(interpreters.constructed().single()).close()
        }
    }
}
