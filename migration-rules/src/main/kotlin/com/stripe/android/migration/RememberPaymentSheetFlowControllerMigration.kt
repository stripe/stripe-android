package com.stripe.android.migration

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression

internal class RememberPaymentSheetFlowControllerMigration(config: Config = Config.empty) : Rule(config) {
    
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Warning,
        "rememberPaymentSheetFlowController is deprecated in Stripe Android SDK v25. Use PaymentSheet.FlowController.Builder instead.",
        Debt.FIVE_MINS
    )
    
    override val autoCorrect = true

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        
        val callName = expression.getCallNameExpression()?.text
        if (callName == "rememberPaymentSheetFlowController") {
            val replacement = generateReplacement(expression)
            if (replacement != null) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        message = "Replace with: $replacement",
                        references = emptyList()
                    )
                )
                
                // Apply auto-correction if enabled
                if (autoCorrect) {
                    val newExpression = KtPsiFactory(expression.project).createExpression(replacement)
                    expression.replace(newExpression)
                }
            }
        }
    }
    
    private fun generateReplacement(expression: KtCallExpression): String? {
        val valueArguments = expression.valueArguments
        val argumentMap = mutableMapOf<String, String>()
        
        // Parse arguments (both named and positional)
        valueArguments.forEachIndexed { index, arg ->
            val argName = arg.getArgumentName()?.asName?.asString()
            val argValue = arg.getArgumentExpression()?.text
            
            if (argValue != null) {
                when {
                    argName != null -> argumentMap[argName] = argValue
                    // Handle positional arguments - FlowController has different order than PaymentSheet
                    index == 0 && argumentMap.isEmpty() -> {
                        // Could be paymentOptionCallback or createIntentCallback depending on overload
                        // We'll need to infer from context or parameter count
                    }
                }
            }
        }
        
        return when {
            // Pattern 1: paymentOptionCallback + paymentResultCallback
            argumentMap.size == 2 && 
            argumentMap.containsKey("paymentOptionCallback") && 
            argumentMap.containsKey("paymentResultCallback") -> {
                val optionCallback = argumentMap["paymentOptionCallback"]
                val resultCallback = argumentMap["paymentResultCallback"]
                "remember($optionCallback, $resultCallback) { PaymentSheet.FlowController.Builder($resultCallback, $optionCallback) }.build()"
            }
            
            // Pattern 2: createIntentCallback + paymentOptionCallback + paymentResultCallback
            argumentMap.size == 3 &&
            argumentMap.containsKey("createIntentCallback") &&
            argumentMap.containsKey("paymentOptionCallback") &&
            argumentMap.containsKey("paymentResultCallback") -> {
                val createCallback = argumentMap["createIntentCallback"]
                val optionCallback = argumentMap["paymentOptionCallback"]
                val resultCallback = argumentMap["paymentResultCallback"]
                "remember($createCallback, $optionCallback, $resultCallback) { " +
                "PaymentSheet.FlowController.Builder($resultCallback, $optionCallback)" +
                ".createIntentCallback($createCallback) }.build()"
            }
            
            // Pattern 3: All four parameters
            argumentMap.size == 4 &&
            argumentMap.containsKey("createIntentCallback") &&
            argumentMap.containsKey("externalPaymentMethodConfirmHandler") &&
            argumentMap.containsKey("paymentOptionCallback") &&
            argumentMap.containsKey("paymentResultCallback") -> {
                val createCallback = argumentMap["createIntentCallback"]
                val externalHandler = argumentMap["externalPaymentMethodConfirmHandler"]
                val optionCallback = argumentMap["paymentOptionCallback"]
                val resultCallback = argumentMap["paymentResultCallback"]
                "remember($createCallback, $externalHandler, $optionCallback, $resultCallback) { " +
                "PaymentSheet.FlowController.Builder($resultCallback, $optionCallback)" +
                ".createIntentCallback($createCallback)" +
                ".externalPaymentMethodConfirmHandler($externalHandler) }.build()"
            }
            
            else -> null // Unsupported pattern, manual migration needed
        }
    }
}