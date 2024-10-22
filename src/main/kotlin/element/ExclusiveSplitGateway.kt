package element

import org.kalasim.Component
import place.Place
import transition.Transition

class ExclusiveSplitCondition(
    val controlOutput: Place,
    val productOutput: Place,
    val default: Boolean,
    val condition: () -> Boolean
)

class ExclusiveSplitGateway(
    id: String,
    value: String?,
    inputControl: Place,
    inputProduct: Place,
    conditions: List<ExclusiveSplitCondition>
) : BPMNElement(id, value) {
    val transitions = conditions.mapIndexed { i, condition ->
        val transition = Transition("${value ?: id}-$i") {
            val controlInputIds = it.getPlace("inputControl").tokens.map { it.ids }.toSet()
            val productInputIds = it.getPlace("inputProduct").tokens.map { it.ids }.toSet()
            val id = controlInputIds.intersect(productInputIds).firstOrNull()
            val conditionResult = condition.default || condition.condition.invoke()

            return@Transition if (id != null && conditionResult) id else null
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