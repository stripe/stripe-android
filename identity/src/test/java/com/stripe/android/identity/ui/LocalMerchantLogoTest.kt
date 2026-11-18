package com.stripe.android.identity.ui

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class LocalMerchantLogoTest {
    private val imageJobContext = ImageJobContext()

    @get:Rule
    val composeRule = createComposeRule(effectContext = imageJobContext)

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `logo loads off Main and is reused across recompositions`() = runScenario {
        val call = provider.openCalls.awaitItem()
        assertThat(call.thread).isNotSameInstanceAs(Looper.getMainLooper().thread)
        waitForText("Loaded 4 0")

        composeRule.runOnIdle { revision.value = 1 }
        waitForText("Loaded 4 1")
        provider.openCalls.expectNoEvents()
    }

    @Test
    fun `changing URI clears the previous logo until the replacement loads`() = runScenario {
        provider.openCalls.awaitItem()
        waitForText("Loaded 4 0")
        val release = CountDownLatch(1)
        provider.gate = release
        try {
            val replacement = Uri.parse("content://identity-logo/replacement")
            composeRule.runOnIdle { uri.value = replacement }
            composeRule.onNodeWithText("Loading 0").assertIsDisplayed()
            assertThat(provider.openCalls.awaitItem().uri).isEqualTo(replacement)
        } finally {
            release.countDown()
        }
        waitForText("Loaded 8 0")
    }

    @Test
    fun `cancelled logo finishing after its replacement cannot restore the old image`() {
        val release = CountDownLatch(1)
        runScenario(initialGate = release) {
            try {
                val original = provider.openCalls.awaitItem()
                provider.gate = null
                val replacement = Uri.parse("content://identity-logo/replacement")
                composeRule.runOnIdle { uri.value = replacement }
                waitForText("Loaded 8 0")
                assertThat(provider.openCalls.awaitItem().uri).isEqualTo(replacement)
                assertThat(original.producer.isCancelled).isTrue()
                assertThat(original.producer.isCompleted).isFalse()

                release.countDown()
                // Wait past the old read, decode, and cancelled producer's exit.
                composeRule.waitUntilWithIdle { original.producer.isCompleted }
                composeRule.onNodeWithText("Loaded 8 0").assertIsDisplayed()
                provider.openCalls.expectNoEvents()
            } finally {
                release.countDown()
            }
        }
    }

    private fun runScenario(
        initialGate: CountDownLatch? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fun image(name: String, size: Int): File {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            try {
                return File(context.cacheDir, name).apply {
                    outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            } finally {
                bitmap.recycle()
            }
        }
        val provider = FakeImageProvider(
            images = mapOf(
                Uri.parse("content://identity-logo/first") to image("first.png", 4),
                Uri.parse("content://identity-logo/replacement") to image("replacement.png", 8),
            ),
            imageJobContext = imageJobContext,
        ).apply {
            gate = initialGate
            attachInfo(context, ProviderInfo().apply { authority = "identity-logo" })
        }
        ShadowContentResolver.registerProviderInternal("identity-logo", provider)
        val uri = mutableStateOf(Uri.parse("content://identity-logo/first"))
        val revision = mutableStateOf(0)
        composeRule.setContent {
            val drawable = rememberLocalDrawable(uri.value)
            Text("${if (drawable == null) "Loading" else "Loaded ${drawable.intrinsicWidth}"} ${revision.value}")
        }
        Scenario(provider, uri, revision).block()
        provider.openCalls.ensureAllEventsConsumed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntilWithIdle {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private data class Scenario(
        val provider: FakeImageProvider,
        val uri: MutableState<Uri>,
        val revision: MutableState<Int>,
    )

    private class FakeImageProvider(
        private val images: Map<Uri, File>,
        private val imageJobContext: ImageJobContext,
    ) : ContentProvider() {
        val openCalls = Turbine<OpenCall>()

        @Volatile
        var gate: CountDownLatch? = null

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val loadGate = gate
            val producer = requireNotNull(imageJobContext.currentJob.get()?.parent)
            openCalls.add(OpenCall(uri, Thread.currentThread(), producer))
            loadGate?.let { check(it.await(10, TimeUnit.SECONDS)) { "Logo load was not released" } }
            return ParcelFileDescriptor.open(images.getValue(uri), ParcelFileDescriptor.MODE_READ_ONLY)
        }

        override fun onCreate() = true
        override fun getType(uri: Uri): String = "image/png"
        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = error("Unexpected query")
        override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Unexpected insert")
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            error("Unexpected delete")
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = error("Unexpected update")

        data class OpenCall(val uri: Uri, val thread: Thread, val producer: Job)
    }

    // Observe the producer's lifecycle without adding a test hook to the image loader.
    private class ImageJobContext : ThreadContextElement<Job?>, AbstractCoroutineContextElement(Key) {
        val currentJob = ThreadLocal<Job?>()

        override fun updateThreadContext(context: CoroutineContext): Job? {
            val previous = currentJob.get()
            currentJob.set(context[Job])
            return previous
        }

        override fun restoreThreadContext(context: CoroutineContext, oldState: Job?) {
            currentJob.set(oldState)
        }

        companion object Key : CoroutineContext.Key<ImageJobContext>
    }
}
