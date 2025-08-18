package com.stripe.android.link

import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LinkControllerPresenterTest {
    private val coordinator: LinkControllerCoordinator = mock()
    private val interactor: LinkControllerInteractor = mock()
    private val presenter = LinkController.Presenter(
        coordinator = coordinator,
        interactor = interactor,
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
}
