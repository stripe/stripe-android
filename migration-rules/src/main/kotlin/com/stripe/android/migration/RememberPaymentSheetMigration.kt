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

internal class RememberPaymentSheetMigration(config: Config = Config.empty) : Rule(config) {
    
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Warning,
        "rememberPaymentSheet is deprecated in Stripe Android SDK v25. Use PaymentSheet.Builder instead.",
        Debt.FIVE_MINS
    )
    
    override val autoCorrect = true

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        
        val callName = expression.getCallNameExpression()?.text
        if (callName == "rememberPaymentSheet") {
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
                    // Handle positional arguments based on common patterns
                    index == 0 && argumentMap.isEmpty() -> argumentMap["paymentResultCallback"] = argValue
                    index == 0 && argumentMap.size == 1 -> argumentMap["createIntentCallback"] = argValue
                    index == 1 && !argumentMap.containsKey("paymentResultCallback") -> argumentMap["paymentResultCallback"] = argValue
                }
            }
        }
        
        return when {
            // Pattern 1: Only paymentResultCallback
            argumentMap.size == 1 && argumentMap.containsKey("paymentResultCallback") -> {
                val callback = argumentMap["paymentResultCallback"]
                "remember($callback) { PaymentSheet.Builder($callback) }.build()"
            }
            
            // Pattern 2: createIntentCallback + paymentResultCallback
            argumentMap.size == 2 && 
            argumentMap.containsKey("createIntentCallback") && 
            argumentMap.containsKey("paymentResultCallback") -> {
                val createCallback = argumentMap["createIntentCallback"]
                val resultCallback = argumentMap["paymentResultCallback"]
                "remember($createCallback, $resultCallback) { PaymentSheet.Builder($resultCallback).createIntentCallback($createCallback) }.build()"
            }
            
            // Pattern 3: All three parameters
            argumentMap.size == 3 &&
            argumentMap.containsKey("createIntentCallback") &&
            argumentMap.containsKey("externalPaymentMethodConfirmHandler") &&
            argumentMap.containsKey("paymentResultCallback") -> {
                val createCallback = argumentMap["createIntentCallback"]
                val externalHandler = argumentMap["externalPaymentMethodConfirmHandler"]
                val resultCallback = argumentMap["paymentResultCallback"]
                "remember($createCallback, $externalHandler, $resultCallback) { " +
                "PaymentSheet.Builder($resultCallback)" +
                ".createIntentCallback($createCallback)" +
                ".externalPaymentMethodConfirmHandler($externalHandler) }.build()"
            }
            
            else -> null // Unsupported pattern, manual migration needed
        }
    }
}