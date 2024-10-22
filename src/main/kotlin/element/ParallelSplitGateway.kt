package element

import org.kalasim.Component
import place.Place
import transition.Transition

class ParallelSplitGateway(
    id: String,
    inputControl: Place,
    outputsControl: List<Place>,
    inputProduct: Place,
    outputsProduct: List<Place>
) : BPMNElement(id, null) {
    val outputs: Int
    init {
        require(outputsControl.count() == outputsProduct.count()) {"Control and Product outputs must be the same number in $id"}
        outputs = outputsControl.count()
    }

    val transition = Transition(id) {
        val controlInputIds = it.getPlace("inputControl").tokens.map { it.ids }.toSet()
        val productInputIds = it.getPlace("inputProduct").tokens.map { it.ids }.toSet()
        return@Transition controlInputIds.intersect(productInputIds).firstOrNull()
    }

    init {
        transition.addPlace("inputControl", inputControl)
        transition.addPlace("inputProduct", inputProduct)
        outputsControl.forEachIndexed { i, outputControl ->
            transition.addPlace("outputControl-$i", outputControl)
        }
        outputsProduct.forEachIndexed { i, outputProduct ->
            transition.addPlace("outputProduct-$i", outputProduct)
        }
    }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val fireableTokenId = transition.fireableTokenId
        if (fireableTokenId != null) { // There is at least one token with the same id in both controlInputPlace and productInputPlace
            // Take tokens from inputs
            val controlToken = transition.getPlace("inputControl").take(fireableTokenId)
            val productToken = transition.getPlace("inputProduct").take(fireableTokenId)

            // Add to each output
            (0 until outputs).forEach { i ->
                transition.getPlace("outputControl-$i").add(controlToken)
                transition.getPlace("outputProduct-$i").add(productToken)
            }
        } else {
            standby()
        }
    }
}