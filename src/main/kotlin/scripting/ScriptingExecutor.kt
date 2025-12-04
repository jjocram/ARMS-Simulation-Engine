package scripting

import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

class ScriptingExecutor {
    private val manager = ScriptEngineManager(this::class.java.classLoader)
    private val engine: ScriptEngine = manager.getEngineByName("kotlin")
    private val defaultImports = arrayOf(
        "kotlin.random.Random",
        "scripting.ScriptContext"
    )

    fun evalString(script: String, context: ScriptContext): Result<Boolean> {
        val bindings = SimpleBindings()
        context.productToken.productProperties.forEach { (key, value) ->
            bindings["product_$key"] = value
        }

        val fullScript = buildString {
            for (import in defaultImports) { append("import $import\n") }
            append(script)
        }

        try {
            // 2. Evaluate the full script string
            val result = engine.eval(fullScript.trimIndent(), bindings)

            // 3. Type check the result
            return when (result) {
                is Boolean -> Result.success(result)
                null -> Result.failure(Exception("Script evaluation returned null. Script must return a Boolean."))
                else -> Result.failure(Exception("Script evaluation failed: Expected Boolean, got ${result::class.simpleName}"))
            }
        } catch (e: Exception) {
            // 4. Catch and report any compilation or runtime exceptions
            return Result.failure(e)
        }
    }
}