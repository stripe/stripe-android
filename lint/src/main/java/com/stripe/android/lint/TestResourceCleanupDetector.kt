package com.stripe.android.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.util.EnumSet

internal class TestResourceCleanupDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UFile::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitFile(node: UFile) {
                if (context.isInTestSourceSet().not()) {
                    return
                }

                node.accept(TestResourceCleanupVisitor(context))
            }
        }
    }

    private class TestResourceCleanupVisitor(
        private val context: JavaContext,
    ) : AbstractUastVisitor() {
        private val trackedVariables = mutableMapOf<CleanupKind, MutableSet<String>>()
        private val instantiations = mutableListOf<Instantiation>()

        override fun visitCallExpression(node: UCallExpression): Boolean {
            node.trackingKind()?.let { trackingKind ->
                node.valueArguments.singleOrNull()?.variableName()?.let { variableName ->
                    trackedVariables.getOrPut(trackingKind) { mutableSetOf() }.add(variableName)
                }
            }

            if (node.kind == UastCallKind.CONSTRUCTOR_CALL && node.isSuperTypeConstructorCall().not()) {
                val constructedClass = node.resolve()?.containingClass
                val cleanupKind = constructedClass?.cleanupKind(context)

                if (
                    cleanupKind != null &&
                    (cleanupKind != CleanupKind.VIEW_MODEL || node.isInsideViewModelFactory().not())
                ) {
                    instantiations.add(
                        Instantiation(
                            call = node,
                            cleanupKind = cleanupKind,
                            variableName = node.assignedVariableName(),
                        )
                    )
                }
            }

            return super.visitCallExpression(node)
        }

        override fun afterVisitFile(node: UFile) {
            instantiations
                .filterNot { it.isTracked(trackedVariables) }
                .forEach { instantiation ->
                    context.report(
                        issue = ISSUE,
                        scope = instantiation.call,
                        location = context.getNameLocation(instantiation.call),
                        message = instantiation.cleanupKind.message,
                    )
                }
        }

        private fun Instantiation.isTracked(
            trackedVariables: Map<CleanupKind, Set<String>>,
        ): Boolean {
            return call.isInsideTrackCall(cleanupKind) ||
                call.isTrackedByScopeFunction(cleanupKind) ||
                variableName in trackedVariables[cleanupKind].orEmpty()
        }

        private fun UCallExpression.isInsideTrackCall(cleanupKind: CleanupKind): Boolean {
            return generateSequence(uastParent) { it.uastParent }
                .filterIsInstance<UCallExpression>()
                .any { parentCall -> parentCall.trackingKind() == cleanupKind }
        }

        private fun UCallExpression.isTrackedByScopeFunction(cleanupKind: CleanupKind): Boolean {
            return generateSequence(uastParent) { it.uastParent }
                .mapNotNull { parent ->
                    when (parent) {
                        is UCallExpression -> parent
                        is UQualifiedReferenceExpression -> parent.selector as? UCallExpression
                        else -> null
                    }
                }
                .filter { parentCall -> parentCall.methodName in SCOPE_FUNCTIONS }
                .any { scopeFunction ->
                    val trackedNames = scopeFunction.valueArguments
                        .filterIsInstance<ULambdaExpression>()
                        .flatMap { lambdaExpression -> scopeFunction.trackedLambdaNames(lambdaExpression) }
                        .ifEmpty { listOf("it", "this") }
                        .toSet()

                    scopeFunction.hasTrackingCall(
                        cleanupKind = cleanupKind,
                        variableNames = trackedNames,
                    )
                }
        }

        private fun UCallExpression.trackedLambdaNames(lambdaExpression: ULambdaExpression): Set<String> {
            val lambdaParameters = lambdaExpression.valueParameters.mapNotNull { parameter ->
                parameter.name.takeIf { it.isNotBlank() }
            }

            return when (methodName) {
                "also", "let" -> lambdaParameters.ifEmpty { listOf("it") }.toSet()
                "apply", "run" -> lambdaParameters.toSet() + "this"
                else -> emptySet()
            }
        }

        private fun UElement.hasTrackingCall(
            cleanupKind: CleanupKind,
            variableNames: Set<String>,
        ): Boolean {
            var foundTrackingCall = false

            accept(
                object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        if (node.trackingKind() == cleanupKind) {
                            val argumentName = node.valueArguments.singleOrNull()?.variableName()

                            if (argumentName in variableNames) {
                                foundTrackingCall = true
                            }
                        }

                        return super.visitCallExpression(node)
                    }
                }
            )

            return foundTrackingCall
        }

        private fun UCallExpression.trackingKind(): CleanupKind? {
            if (methodName != "track") {
                return null
            }

            // Match the cleanup rule by its simple class name rather than a fully-qualified name.
            // The rule is enforced repo-wide, but the canonical rules live in modules that lower
            // modules (e.g. 3ds2sdk, financial-connections) can't depend on, so those modules keep a
            // local copy. Matching by name lets any `ViewModelStoreTestRule`/`CleanupTestRule` satisfy
            // the check regardless of package.
            return when (resolve()?.containingClass?.name) {
                CLEANUP_TEST_RULE_NAME -> CleanupKind.CLOSEABLE
                VIEW_MODEL_STORE_TEST_RULE_NAME -> CleanupKind.VIEW_MODEL
                else -> null
            }
        }

        private fun UElement.variableName(): String? {
            if (this is USimpleNameReferenceExpression) {
                return identifier
            }

            return asSourceString()
                .trim()
                .removeSurrounding("(", ")")
                .takeIf { name -> name.matches(VARIABLE_NAME_REGEX) }
        }

        private fun UCallExpression.assignedVariableName(): String? {
            return generateSequence(uastParent) { it.uastParent }
                .filterIsInstance<UVariable>()
                .firstOrNull()
                ?.name
        }

        private fun UCallExpression.isInsideViewModelFactory(): Boolean {
            return generateSequence(uastParent) { it.uastParent }
                .any { parent ->
                    when (parent) {
                        is UCallExpression -> parent.methodName in VIEW_MODEL_FACTORY_FUNCTIONS
                        is UMethod -> parent.isViewModelProviderFactoryCreate()
                        else -> false
                    }
                }
        }

        private fun UMethod.isViewModelProviderFactoryCreate(): Boolean {
            return name == "create" && javaPsi.containingClass?.let { containingClass ->
                this@TestResourceCleanupVisitor.context.evaluator.extendsClass(
                    containingClass,
                    VIEW_MODEL_PROVIDER_FACTORY,
                    false,
                )
            } == true
        }

        private fun UCallExpression.isSuperTypeConstructorCall(): Boolean {
            var current = sourcePsi

            while (current != null) {
                if (current is KtSuperTypeCallEntry) {
                    return true
                }

                current = current.parent
            }

            return false
        }
    }

    private enum class CleanupKind(
        val message: String,
    ) {
        CLOSEABLE(
            "Closeable test instances must be tracked with `CleanupTestRule.track(...)` " +
                "so they are closed when the test finishes."
        ),
        VIEW_MODEL(
            "ViewModel test instances must be tracked with `ViewModelStoreTestRule.track(...)` " +
                "so they are cleared when the test finishes."
        )
    }

    private data class Instantiation(
        val call: UCallExpression,
        val cleanupKind: CleanupKind,
        val variableName: String?,
    )

    companion object {
        private const val CLEANUP_TEST_RULE_NAME = "CleanupTestRule"
        private const val VIEW_MODEL = "androidx.lifecycle.ViewModel"
        private const val VIEW_MODEL_PROVIDER_FACTORY = "androidx.lifecycle.ViewModelProvider.Factory"
        private const val VIEW_MODEL_STORE_TEST_RULE_NAME = "ViewModelStoreTestRule"

        private val SCOPE_FUNCTIONS = setOf("also", "apply", "let", "run")
        private val VIEW_MODEL_FACTORY_FUNCTIONS = setOf("initializer", "viewModelFactory")
        private val VARIABLE_NAME_REGEX = Regex("[A-Za-z_][A-Za-z0-9_]*")

        private val IMPLEMENTATION = Implementation(
            TestResourceCleanupDetector::class.java,
            EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
        )

        @JvmField
        val ISSUE = Issue.create(
            id = "TestResourceCleanup",
            priority = 8,
            briefDescription = "Closeable and ViewModel test instances must be cleaned up",
            explanation = """
                Tests that directly create closeable objects or ViewModels can leak work into later tests when the
                instance is not closed or cleared. Track closeable instances with CleanupTestRule.track(...) and
                ViewModels with ViewModelStoreTestRule.track(...).
            """,
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            androidSpecific = false,
            implementation = IMPLEMENTATION
        )

        private fun JavaContext.isInTestSourceSet(): Boolean {
            val path = file.path.replace('\\', '/')
            return "/src/test/" in path || "/src/androidTest/" in path
        }

        private fun PsiClass.cleanupKind(context: JavaContext): CleanupKind? {
            if (qualifiedName == null || isTestDouble()) {
                return null
            }

            if (context.evaluator.extendsClass(this, VIEW_MODEL, false)) {
                return CleanupKind.VIEW_MODEL
            }

            if (hasNoArgumentCloseMethod() && isInteractor()) {
                return CleanupKind.CLOSEABLE
            }

            return null
        }

        private fun PsiClass.hasNoArgumentCloseMethod(): Boolean {
            return findMethodsByName("close", true).any { method ->
                method.parameterList.parametersCount == 0
            }
        }

        private fun PsiClass.isTestDouble(): Boolean {
            return name?.let { className ->
                className.startsWith("Fake") ||
                    className.startsWith("Mock") ||
                    className.startsWith("Test")
            } ?: false
        }

        private fun PsiClass.isInteractor(): Boolean {
            return name?.endsWith("Interactor") == true ||
                supers.any { superClass -> superClass.name?.endsWith("Interactor") == true }
        }
    }
}
