package com.stripe.android.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Scope
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("LargeClass")
class TestResourceCleanupDetectorTest {
    @Test
    fun `issue is registered for all source analysis`() {
        assertEquals(
            java.util.EnumSet.of(Scope.JAVA_FILE),
            TestResourceCleanupDetector.ISSUE.implementation.scope,
        )
    }

    @Test
    fun `should detect coroutine scope in tests that is not tracked`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope

                    class ExampleTest {
                        fun test() {
                            val scope = CoroutineScope()
                        }
                    }
                """
            ),
            coroutineScopeStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("CoroutineScope instances must be tracked")
    }

    @Test
    fun `should not detect coroutine scope in tests tracked with cleanup rule`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.testing.CleanupTestRule
                    import kotlinx.coroutines.CoroutineScope

                    class ExampleTest {
                        private val cleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

                        fun test() {
                            val scope = cleanupRule.track(CoroutineScope())
                        }
                    }
                """
            ),
            coroutineScopeStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should detect coroutine scope in production that is not cancelled`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope

                    class ExampleInteractor {
                        private val scope = CoroutineScope()
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("CoroutineScope instances must be.*cancelled in production code")
    }

    @Test
    fun `should not detect coroutine scope in production that is cancelled`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope

                    class ExampleInteractor {
                        private val scope = CoroutineScope()

                        fun close() {
                            scope.cancel()
                        }
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect coroutine scope created with an already-cancelled job`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.Job

                    class ExampleInteractor {
                        private val scope = CoroutineScope(Job().apply { cancel() })
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect coroutine scope whose backing job is cancelled`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.SupervisorJob

                    class ExampleInteractor {
                        private val supervisorJob = SupervisorJob()
                        private val scope = CoroutineScope(supervisorJob)

                        fun close() {
                            supervisorJob.cancel()
                        }
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect coroutine scope whose launched job is cancelled`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch

                    class ExampleInteractor(
                        private val scope: CoroutineScope = CoroutineScope(),
                    ) {
                        private var job: Job? = null

                        fun start() {
                            job = scope.launch {}
                        }

                        fun close() {
                            job?.cancel()
                            job = null
                        }
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should detect coroutine scope whose launched job is not cancelled`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch

                    class ExampleInteractor(
                        private val scope: CoroutineScope = CoroutineScope(),
                    ) {
                        private var job: Job? = null

                        fun start() {
                            job = scope.launch {}
                        }
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("CoroutineScope instances must be.*cancelled in production code")
    }

    @Test
    fun `should detect coroutine scope with managed and unmanaged launched jobs`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.Job
                    import kotlinx.coroutines.launch

                    class ExampleInteractor(
                        private val scope: CoroutineScope = CoroutineScope(),
                    ) {
                        private var job: Job? = null

                        fun start() {
                            job = scope.launch {}
                            scope.launch {}
                        }

                        fun close() {
                            job?.cancel()
                            job = null
                        }
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("CoroutineScope instances must be.*cancelled in production code")
    }

    @Test
    fun `should detect derived coroutine scope whose owner does not cancel it`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/OwnerFactory.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.childScope

                    class Owner(
                        private val coroutineScope: CoroutineScope,
                    ) {
                        fun close() {}
                    }

                    object OwnerFactory {
                        fun create(parentScope: CoroutineScope): Owner {
                            val coroutineScope = parentScope.childScope()
                            return Owner(coroutineScope)
                        }
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("CoroutineScope instances must be.*cancelled in production code")
    }

    @Test
    fun `should not detect derived coroutine scope whose owner cancels it with named arguments`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/OwnerFactory.kt",
                """
                    package com.stripe.android.example

                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.childScope

                    class Owner(
                        private val id: String,
                        private val scope: CoroutineScope,
                    ) {
                        fun close() {
                            scope.cancel()
                        }
                    }

                    object OwnerFactory {
                        fun create(parentScope: CoroutineScope): Owner {
                            val coroutineScope = parentScope.childScope()
                            return Owner(
                                scope = coroutineScope,
                                id = "owner",
                            )
                        }
                    }
                """
            ).indented(),
            coroutineScopeStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should detect closeable instance that is not tracked`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.CloseableInteractor

                    class CloseableInteractorTest {
                        fun test() {
                            val interactor = CloseableInteractor()
                        }
                    }
                """
            ),
            closeableInteractorStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("Closeable test instances must be tracked")
    }

    @Test
    fun `should detect interactor whose close method comes from interface`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.DefaultExampleInteractor

                    class DefaultExampleInteractorTest {
                        fun test() {
                            val interactor = DefaultExampleInteractor()
                        }
                    }
                """
            ),
            interactorInterfaceStub(),
            defaultInteractorStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("Closeable test instances must be tracked")
    }

    @Test
    fun `should ignore closeable instance that is not an interactor`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.CloseableScreen

                    class CloseableScreenTest {
                        fun test() {
                            val screen = CloseableScreen()
                        }
                    }
                """
            ),
            closeableScreenStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect closeable instance tracked directly`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.CloseableInteractor
                    import com.stripe.android.testing.CleanupTestRule

                    class CloseableInteractorTest {
                        private val cleanupRule = CleanupTestRule(CloseableInteractor::close)

                        fun test() {
                            val interactor = cleanupRule.track(CloseableInteractor())
                        }
                    }
                """
            ),
            closeableInteractorStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect closeable instance tracked with also`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.CloseableInteractor
                    import com.stripe.android.testing.CleanupTestRule

                    class CloseableInteractorTest {
                        private val cleanupRule = CleanupTestRule(CloseableInteractor::close)

                        fun test() {
                            val interactor = CloseableInteractor()
                                .also { cleanupRule.track(it) }
                        }
                    }
                """
            ),
            closeableInteractorStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect closeable instance tracked after assignment`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.CloseableInteractor
                    import com.stripe.android.testing.CleanupTestRule

                    class CloseableInteractorTest {
                        private val cleanupRule = CleanupTestRule(CloseableInteractor::close)

                        fun test() {
                            val interactor = CloseableInteractor()
                            cleanupRule.track(interactor)
                        }
                    }
                """
            ),
            closeableInteractorStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should detect viewModel instance that is not tracked`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.ExampleViewModel

                    class ExampleViewModelTest {
                        fun test() {
                            val viewModel = ExampleViewModel()
                        }
                    }
                """
            ),
            viewModelStub(),
            exampleViewModelStub(),
            viewModelStoreTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectErrorCount(1)
            .expectMatches("ViewModel test instances must be tracked")
    }

    @Test
    fun `should not detect viewModel instance tracked with viewModelStoreTestRule`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.ExampleViewModel
                    import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule

                    class ExampleViewModelTest {
                        private val viewModelStoreRule = ViewModelStoreTestRule()

                        fun test() {
                            val viewModel = ExampleViewModel()
                                .also { viewModelStoreRule.track(it) }
                        }
                    }
                """
            ),
            viewModelStub(),
            exampleViewModelStub(),
            viewModelStoreTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect viewModel tracked with viewModelStoreTestRule from another package`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.ExampleViewModel
                    import com.stripe.android.example.testing.ViewModelStoreTestRule

                    class ExampleViewModelTest {
                        private val viewModelStoreRule = ViewModelStoreTestRule()

                        fun test() {
                            val viewModel = ExampleViewModel()
                                .also { viewModelStoreRule.track(it) }
                        }
                    }
                """
            ),
            viewModelStub(),
            exampleViewModelStub(),
            localViewModelStoreTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should not detect viewModel instance returned from factory result that is tracked`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.ExampleViewModel
                    import com.stripe.android.example.TestViewModelFactory
                    import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule

                    class ExampleViewModelTest {
                        private val viewModelStoreRule = ViewModelStoreTestRule()

                        fun test() {
                            val viewModel = TestViewModelFactory.create {
                                ExampleViewModel()
                            }
                            viewModelStoreRule.track(viewModel)
                        }
                    }
                """
            ),
            viewModelStub(),
            exampleViewModelStub(),
            testViewModelFactoryStub(),
            viewModelStoreTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should ignore viewModel instance created by viewModelProvider factory`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import androidx.lifecycle.ViewModel
                    import androidx.lifecycle.ViewModelProvider
                    import com.stripe.android.example.ExampleViewModel

                    class ExampleViewModelTest {
                        fun test(): ViewModelProvider.Factory {
                            return object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return ExampleViewModel() as T
                                }
                            }
                        }
                    }
                """
            ),
            viewModelStub(),
            viewModelProviderFactoryStub(),
            exampleViewModelStub(),
            viewModelStoreTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should ignore viewModel instance created by viewModelFactory initializer`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import androidx.lifecycle.viewmodel.initializer
                    import androidx.lifecycle.viewmodel.viewModelFactory
                    import com.stripe.android.example.ExampleViewModel

                    class ExampleViewModelTest {
                        fun test() = viewModelFactory {
                            initializer {
                                ExampleViewModel()
                            }
                        }
                    }
                """
            ),
            viewModelStub(),
            viewModelProviderFactoryStub(),
            viewModelFactoryDslStub(),
            exampleViewModelStub(),
            viewModelStoreTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should ignore production source`() {
        lint().files(
            kotlin(
                "src/main/java/com/stripe/android/example/CloseableInteractorUsage.kt",
                """
                    package com.stripe.android.example

                    class CloseableInteractorUsage {
                        fun create() {
                            CloseableInteractor()
                        }
                    }
                """
            ).indented(),
            closeableInteractorStub(),
            cleanupTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun `should ignore test doubles`() {
        lint().files(
            testFile(
                """
                    package com.stripe.android.example

                    import com.stripe.android.example.FakeViewModel

                    class FakeViewModelTest {
                        fun test() {
                            val viewModel = FakeViewModel()
                        }
                    }
                """
            ),
            viewModelStub(),
            fakeViewModelStub(),
            viewModelStoreTestRuleStub(),
        )
            .issues(TestResourceCleanupDetector.ISSUE)
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    private fun testFile(source: String) = kotlin(
        "src/test/java/com/stripe/android/example/ExampleTest.kt",
        source
    ).indented()

    private fun closeableInteractorStub() = kotlin(
        "src/main/java/com/stripe/android/example/CloseableInteractor.kt",
        """
            package com.stripe.android.example

            class CloseableInteractor {
                fun close() {}
            }
        """
    ).indented()

    private fun coroutineScopeStub() = kotlin(
        "src/main/java/kotlinx/coroutines/CoroutineScope.kt",
        """
            package kotlinx.coroutines

            interface CoroutineScope {
                fun cancel() {}
            }

            interface Job {
                fun cancel() {}
            }

            fun Job(): Job = error("Not implemented")

            fun SupervisorJob(): Job = error("Not implemented")

            fun CoroutineScope(context: Any? = null): CoroutineScope = error("Not implemented")

            fun CoroutineScope.launch(block: () -> Unit): Job = error("Not implemented")

            fun CoroutineScope.childScope(): CoroutineScope = CoroutineScope()
        """
    ).indented()

    private fun interactorInterfaceStub() = kotlin(
        "src/main/java/com/stripe/android/example/ExampleInteractor.kt",
        """
            package com.stripe.android.example

            interface ExampleInteractor {
                fun close()
            }
        """
    ).indented()

    private fun defaultInteractorStub() = kotlin(
        "src/main/java/com/stripe/android/example/DefaultExampleInteractor.kt",
        """
            package com.stripe.android.example

            class DefaultExampleInteractor : ExampleInteractor {
                override fun close() {}
            }
        """
    ).indented()

    private fun closeableScreenStub() = kotlin(
        "src/main/java/com/stripe/android/example/CloseableScreen.kt",
        """
            package com.stripe.android.example

            class CloseableScreen {
                fun close() {}
            }
        """
    ).indented()

    private fun cleanupTestRuleStub() = kotlin(
        "src/test/java/com/stripe/android/testing/CleanupTestRule.kt",
        """
            package com.stripe.android.testing

            class CleanupTestRule<T>(
                private val cleanup: T.() -> Unit,
            ) {
                fun track(objectToCleanup: T): T = objectToCleanup
            }
        """
    ).indented()

    private fun viewModelStub() = kotlin(
        "src/main/java/androidx/lifecycle/ViewModel.kt",
        """
            package androidx.lifecycle

            open class ViewModel
        """
    ).indented()

    private fun viewModelProviderFactoryStub() = kotlin(
        "src/main/java/androidx/lifecycle/ViewModelProvider.kt",
        """
            package androidx.lifecycle

            class ViewModelProvider {
                interface Factory {
                    fun <T : ViewModel> create(modelClass: Class<T>): T
                }
            }
        """
    ).indented()

    private fun viewModelFactoryDslStub() = kotlin(
        "src/main/java/androidx/lifecycle/viewmodel/ViewModelFactoryDsl.kt",
        """
            package androidx.lifecycle.viewmodel

            import androidx.lifecycle.ViewModel
            import androidx.lifecycle.ViewModelProvider

            fun viewModelFactory(builder: InitializerViewModelFactoryBuilder.() -> Unit): ViewModelProvider.Factory {
                return object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        error("Not implemented")
                    }
                }
            }

            class InitializerViewModelFactoryBuilder

            fun <T : ViewModel> InitializerViewModelFactoryBuilder.initializer(initializer: () -> T) {}
        """
    ).indented()

    private fun exampleViewModelStub() = kotlin(
        "src/main/java/com/stripe/android/example/ExampleViewModel.kt",
        """
            package com.stripe.android.example

            import androidx.lifecycle.ViewModel

            class ExampleViewModel : ViewModel()
        """
    ).indented()

    private fun testViewModelFactoryStub() = kotlin(
        "src/test/java/com/stripe/android/example/TestViewModelFactory.kt",
        """
            package com.stripe.android.example

            import androidx.lifecycle.ViewModel

            object TestViewModelFactory {
                fun <T : ViewModel> create(createViewModel: () -> T): T {
                    return createViewModel()
                }
            }
        """
    ).indented()

    private fun fakeViewModelStub() = kotlin(
        "src/test/java/com/stripe/android/example/FakeViewModel.kt",
        """
            package com.stripe.android.example

            import androidx.lifecycle.ViewModel

            class FakeViewModel : ViewModel()
        """
    ).indented()

    private fun viewModelStoreTestRuleStub() = kotlin(
        "src/test/java/com/stripe/android/paymentsheet/utils/ViewModelStoreTestRule.kt",
        """
            package com.stripe.android.paymentsheet.utils

            import androidx.lifecycle.ViewModel

            class ViewModelStoreTestRule {
                fun <T : ViewModel> track(viewModel: T): T = viewModel
            }
        """
    ).indented()

    private fun localViewModelStoreTestRuleStub() = kotlin(
        "src/test/java/com/stripe/android/example/testing/ViewModelStoreTestRule.kt",
        """
            package com.stripe.android.example.testing

            import androidx.lifecycle.ViewModel

            class ViewModelStoreTestRule {
                fun <T : ViewModel> track(viewModel: T): T = viewModel
            }
        """
    ).indented()
}
