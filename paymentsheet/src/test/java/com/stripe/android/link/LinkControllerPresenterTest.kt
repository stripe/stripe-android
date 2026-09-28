package com.stripe.android.link

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.link.injection.LinkControllerPresenterComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LinkControllerPresenterTest {
    private val coordinator: LinkControllerCoordinator = mock()
    private val interactor: LinkControllerInteractor = mock()
    private val presenterComponentFactory: LinkControllerPresenterComponent.Factory = mock()
    private val presenter = LinkController.Presenter(
        coordinator = coordinator,
        interactor = interactor,
    )
    private val controller = LinkController(
        interactor = interactor,
        presenterComponentFactory = presenterComponentFactory,
    )

    @Test
    fun `connects interactor with coordinator`() = runTest {
        val launcher: ActivityResultLauncher<LinkActivityContract.Args> = mock()
        whenever(coordinator.linkActivityResultLauncher).doReturn(launcher)
        val email = "foo@bar.com"

        presenter.authenticate(email)

        verify(interactor).authenticate(
            launcher = launcher,
            email = email
        )
    }

    @Test
    fun `present() delegates to presentFull on the interactor`() = runTest {
        val launcher: ActivityResultLauncher<LinkActivityContract.Args> = mock()
        whenever(coordinator.linkActivityResultLauncher).doReturn(launcher)

        presenter.present()

        verify(interactor).presentFull(launcher = launcher)
    }
}
