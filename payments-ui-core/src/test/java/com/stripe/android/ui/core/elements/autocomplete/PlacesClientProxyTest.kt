package com.stripe.android.ui.core.elements.autocomplete

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPhotoResponse
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.IsPlacesAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(RobolectricTestRunner::class)
class PlacesClientProxyTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create returns default client when google places available`() {
        val client = PlacesClientProxy.create(
            context = mock(),
            googlePlacesApiKey = "abc123",
            isPlacesAvailable = object : IsPlacesAvailable {
                override fun invoke(): Boolean {
                    return true
                }
            },
            clientFactory = { mock() },
            initializer = { /* no-op */ }
        )

        assertThat(client).isInstanceOf(DefaultPlacesClientProxy::class.java)
    }

    @Test
    fun `create returns unsupported client when google places not available`() {
        val client = PlacesClientProxy.create(
            context = mock(),
            googlePlacesApiKey = "abc123",
            isPlacesAvailable = object : IsPlacesAvailable {
                override fun invoke(): Boolean {
                    return false
                }
            },
            clientFactory = { mock() },
            initializer = { /* no-op */ }
        )

        assertThat(client).isInstanceOf(UnsupportedPlacesClientProxy::class.java)
    }

    @Test
    fun `findAutocompletePredictions returns results with limit`() =
        runTest(UnconfinedTestDispatcher()) {
            val client = createGooglePlacesClient(
                onFindAutocompletePredictions = {
                    Tasks.forResult(
                        FindAutocompletePredictionsResponse.newInstance(
                            listOf(
                                AutocompletePrediction.builder("1").build(),
                                AutocompletePrediction.builder("2").build(),
                                AutocompletePrediction.builder("3").build(),
                                AutocompletePrediction.builder("4").build(),
                                AutocompletePrediction.builder("5").build()
                            )
                        )
                    )
                }
            )
            val proxy = PlacesClientProxy.create(
                context = mock(),
                googlePlacesApiKey = "abc123",
                isPlacesAvailable = object : IsPlacesAvailable {
                    override fun invoke(): Boolean {
                        return true
                    }
                },
                clientFactory = { client },
                initializer = { /* no-op */ }
            )

            val predictions = proxy.findAutocompletePredictions(
                "some query",
                "country",
                3
            )

            runCurrent()

            assertThat(predictions.getOrNull()?.autocompletePredictions?.size)
                .isEqualTo(3)
        }

    @Test
    fun `fetchPlace returns place`() =
        runTest(UnconfinedTestDispatcher()) {
            val client = createGooglePlacesClient(
                onFetchPlace = {
                    Tasks.forResult(
                        FetchPlaceResponse.newInstance(
                            Place.builder().build()
                        )
                    )
                }
            )
            val proxy = PlacesClientProxy.create(
                context = mock(),
                googlePlacesApiKey = "abc123",
                isPlacesAvailable = object : IsPlacesAvailable {
                    override fun invoke(): Boolean {
                        return true
                    }
                },
                clientFactory = { client },
                initializer = { /* no-op */ }
            )

            val place = proxy.fetchPlace(
                "placeId"
            )

            runCurrent()

            assertThat(place.getOrNull()?.place)
                .isNotNull()
        }

    @Test
    fun `getPlacesPoweredByGoogleDrawable returns drawable when places is available`() {
        val drawable = PlacesClientProxy.getPlacesPoweredByGoogleDrawable(
            isSystemDarkTheme = true,
            isPlacesAvailable = object : IsPlacesAvailable {
                override fun invoke(): Boolean {
                    return true
                }
            }
        )
        assertThat(drawable).isNotNull()
    }

    @Test(expected = IllegalStateException::class)
    fun `getPlacesPoweredByGoogleDrawable returns null when places is not available`() {
        PlacesClientProxy.getPlacesPoweredByGoogleDrawable(
            isSystemDarkTheme = true,
            isPlacesAvailable = object : IsPlacesAvailable {
                override fun invoke(): Boolean {
                    return false
                }
            }
        )
    }

    private fun createGooglePlacesClient(
        onFetchPlace: () -> Task<FetchPlaceResponse> = {
            Tasks.forCanceled()
        },
        onFindAutocompletePredictions: () -> Task<FindAutocompletePredictionsResponse> = {
            Tasks.forCanceled()
        }
    ): PlacesClient {
        val client = object : PlacesClient {
            override fun fetchPhoto(
                p0: FetchPhotoRequest
            ): Task<FetchPhotoResponse> {
                return Tasks.forCanceled()
            }

            override fun fetchPlace(
                p0: FetchPlaceRequest
            ): Task<FetchPlaceResponse> {
                return onFetchPlace()
            }

            override fun findAutocompletePredictions(
                p0: FindAutocompletePredictionsRequest
            ): Task<FindAutocompletePredictionsResponse> {
                return onFindAutocompletePredictions()
            }

            override fun findCurrentPlace(
                p0: FindCurrentPlaceRequest
            ): Task<FindCurrentPlaceResponse> {
                return Tasks.forCanceled()
            }
        }
        return client
    }
}
