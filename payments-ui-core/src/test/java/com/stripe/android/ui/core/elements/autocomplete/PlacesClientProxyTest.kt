package com.stripe.android.ui.core.elements.autocomplete

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPhotoResponse
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.IsOpenRequest
import com.google.android.libraries.places.api.net.IsOpenResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchByTextResponse
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.SearchNearbyResponse
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.elements.IsPlacesAvailable
import com.stripe.android.utils.isInstanceOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlacesClientProxyTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

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
            initializer = { /* no-op */ },
            errorReporter = FakeErrorReporter(),
        )

        assertThat(client).isInstanceOf<DefaultPlacesClientProxy>()
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
            initializer = { /* no-op */ },
            errorReporter = FakeErrorReporter(),
        )

        assertThat(client).isInstanceOf<UnsupportedPlacesClientProxy>()
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
                initializer = { /* no-op */ },
                errorReporter = FakeErrorReporter(),
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
                initializer = { /* no-op */ },
                errorReporter = FakeErrorReporter(),
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

            override fun fetchResolvedPhotoUri(p0: FetchResolvedPhotoUriRequest): Task<FetchResolvedPhotoUriResponse?> {
                throw AssertionError("Not expected")
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

            override fun isOpen(p0: IsOpenRequest): Task<IsOpenResponse> {
                return Tasks.forCanceled()
            }

            override fun searchByText(p0: SearchByTextRequest): Task<SearchByTextResponse> {
                return Tasks.forCanceled()
            }

            override fun searchNearby(p0: SearchNearbyRequest): Task<SearchNearbyResponse?> {
                throw AssertionError("Not expected")
            }

            override fun zzb(
                p0: FetchPlaceRequest,
                p1: Int
            ): Task<*> {
                throw AssertionError("Not expected")
            }

            override fun zzd(
                p0: FindAutocompletePredictionsRequest,
                p1: Int
            ): Task<*> {
                throw AssertionError("Not expected")
            }
        }
        return client
    }
}
