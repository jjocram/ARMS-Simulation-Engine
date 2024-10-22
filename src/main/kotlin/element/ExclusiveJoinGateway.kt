package element

import org.kalasim.Component
import place.Place
import transition.Transition

class ExclusiveJoinGateway(
    id: String,
    value: String?,
    inputsControl: List<Place>,
    outputControl: Place,
    inputsProduct: List<Place>,
    outputProduct: Place
) : BPMNElement(id, value) {
    val inputs: Int

    init {
        require(inputsControl.count() == inputsProduct.count()) { "Control and Product inputs must be the same number in $id" }
        inputs = inputsControl.count()
    }

    val transitions = (0 until inputs)
        .map { i ->
            val transition = Transition("${value ?: id}-$i") {
                val controlInputIds = it.getPlace("inputControl").tokens.map { it.ids }.toSet()
                val productInputIds = it.getPlace("inputProduct").tokens.map { it.ids }.toSet()
                return@Transition controlInputIds.intersect(productInputIds).firstOrNull()
            }

            transition.addPlace("inputControl", inputsControl[i])
            transition.addPlace("inputProduct", inputsProduct[i])
            transition.addPlace("outputControl", outputControl)
            transition.addPlace("outputProduct", outputProduct)

            return@map transition
        }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val firingTransition = transitions.firstOrNull { it.fireableTokenId != null }
        if (firingTransition != null) {
            val tokenId = firingTransition.fireableTokenId!!

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