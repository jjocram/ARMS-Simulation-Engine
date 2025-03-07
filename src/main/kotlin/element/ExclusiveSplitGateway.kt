package element

import org.kalasim.Component
import place.Place
import scripting.ScriptContext
import scripting.ScriptingExecutor
import token.ProductToken
import transition.Transition

class ExclusiveSplitCondition(
    val controlOutput: Place,
    val productOutput: Place,
    val default: Boolean,
    val scriptCode: String //() -> Boolean
)

class ExclusiveSplitGateway(
    id: String,
    value: String?,
    inputControl: Place,
    inputProduct: Place,
    conditions: List<ExclusiveSplitCondition>
) : BPMNElement(id, value) {

    private val scriptingExecutor = ScriptingExecutor()

    val transitions = conditions.mapIndexed { i, condition ->
        val transition = Transition("${value ?: id}-$i") {
            val controlInputIds = it.getPlace("inputControl").tokens.map { it.ids }.toSet()
            val productInputIds = it.getPlace("inputProduct").tokens.map { it.ids }.toSet()
            val id = controlInputIds.intersect(productInputIds).firstOrNull()

            if (id != null) {
                val conditionContext = ScriptContext(it.getPlace("inputProduct").tokens.first { it.ids == id } as ProductToken)

                val scriptOutput = scriptingExecutor.evalString(condition.scriptCode, conditionContext).getOrThrow()

                val conditionResult = condition.default || scriptOutput

                return@Transition if (conditionResult) id else null
            } else {
                return@Transition null
            }
        }

        transition.addPlace("inputControl", inputControl)
        transition.addPlace("inputProduct", inputProduct)
        transition.addPlace("outputControl", condition.controlOutput)
        transition.addPlace("outputProduct", condition.productOutput)

        return@mapIndexed transition
    }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val transitionAndId = transitions.mapNotNull {
            val id = it.fireableTokenId
            return@mapNotNull if (id != null) Pair(it, id) else null
        }.firstOrNull()
        if (transitionAndId != null) {
            val firingTransition = transitionAndId.first
            val tokenId = transitionAndId.second

            // Move from control input to control output
            val controlToken = firingTransition.getPlace("inputControl").take(tokenId)
            firingTransition.getPlace("outputControl").add(controlToken)

            // Move from product input to product output
            val productToken = firingTransition.getPlace("inputProduct").take(tokenId)
            firingTransition.getPlace("outputProduct").add(productToken)
        } else {
            standby()
        }
    }
}