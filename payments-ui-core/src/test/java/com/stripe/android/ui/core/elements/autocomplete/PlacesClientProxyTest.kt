package com.stripe.android.ui.core.elements.autocomplete

import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlacesClientProxyTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @After
    fun tearDown() {
        PlacesClientProxy.override = null
    }

    @Test
    fun `create returns unsupported client`() {
        val client = PlacesClientProxy.create(
            context = mock(),
            googlePlacesApiKey = "abc123",
            errorReporter = FakeErrorReporter(),
        )

        assertThat(client).isInstanceOf(UnsupportedPlacesClientProxy::class.java)
    }

    @Test
    fun `getPlacesPoweredByGoogleDrawable always returns null`() {
        val drawable = PlacesClientProxy.getPlacesPoweredByGoogleDrawable(isSystemDarkTheme = true)
        assertThat(drawable).isNull()
    }

    @Test(expected = IllegalStateException::class)
    fun `findAutocompletePredictions throws in debug builds`() = runTest {
        val proxy = PlacesClientProxy.create(
            context = mock(),
            googlePlacesApiKey = "abc123",
            errorReporter = FakeErrorReporter(),
        )
        proxy.findAutocompletePredictions("query", "country", 3)
    }
}
