package scripting

import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

class ScriptingExecutor {
    private val engine = ScriptEngineManager().getEngineByExtension("kts")

    fun evalString(script: String, context: ScriptContext): Result<Boolean> {
        val bindings = SimpleBindings()
        bindings.put("ctx", context)

        val result = engine.eval(script.trimIndent(), bindings)

        return when (result) {
            is Boolean -> Result.success(result)
            else -> Result.failure(Exception("Script evaluation failed"))
        }
    }
}