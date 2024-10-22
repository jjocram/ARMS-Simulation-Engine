package element

import org.kalasim.Component
import place.Place
import transition.Transition

class ParallelJoinGateway(
    id: String,
    inputsControl: List<Place>,
    outputControl: Place,
    inputsProduct: List<Place>,
    outputProduct: Place
) : BPMNElement(id, null) {
    val inputs: Int

    init {
        require(inputsProduct.count() == inputsControl.count()) { "Control and Product inputs must be the same number in $id" }
        inputs = inputsProduct.count()
    }

    val transition = Transition(id) { t ->
        val controlInputsIds = (0 until inputs)
            .map { i -> t.getPlace("inputControl-$i") }
            .map { it.tokens.toSet() }
            .reduce { acc, set -> acc.intersect(set) }
            .map { it.ids }
            .toSet()

        val productInputsIds = (0 until inputs)
            .map { i -> t.getPlace("inputProduct-$i") }
            .map { it.tokens.toSet() }
            .reduce { acc, set -> acc.intersect(set) }
            .map { it.ids }
            .toSet()

        return@Transition controlInputsIds.intersect(productInputsIds).firstOrNull()
    }

    init {
        inputsControl.forEachIndexed { i, inputControl ->
            transition.addPlace("inputControl-$i", inputControl)
        }
        inputsProduct.forEachIndexed { i, inputProduct ->
            transition.addPlace("inputProduct-$i", inputProduct)
        }
        transition.addPlace("outputControl", outputControl)
        transition.addPlace("outputProduct", outputProduct)
    }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val fireableTokenId = transition.fireableTokenId
        if (fireableTokenId != null) {
            // Take token from all inputs and keep only one (they are all the same)
            val controlToken = (0 until inputs)
                .map { i -> transition.getPlace("inputControl-$i").take(fireableTokenId) }
                .first()
            val productToken = (0 until inputs)
                .map { i -> transition.getPlace("inputProduct-$i").take(fireableTokenId) }
                .first()

            // Add tokens to outputs
            transition.getPlace("outputControl").add(controlToken)
            transition.getPlace("outputProduct").add(productToken)
        } else {
            standby()
        }
    }
}