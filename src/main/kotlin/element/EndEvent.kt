package element

import org.kalasim.Component
import place.Place
import transition.Transition

class EndEvent(
    id: String,
    value: String?,
    inputControl: Place,
    outputControl: Place,
    inputProduct: Place,
    outputProduct: Place
) : BPMNElement(id, value) {
    private val transition =
        Transition(value ?: id) { return@Transition it.connections.map { it.input }.all { it.isNotEmpty } }

    init {
        transition.addConnection(inputControl, outputControl)
        transition.addConnection(inputProduct, outputProduct)
    }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        if (transition.canFire) {
            transition.fireImmediately()
        } else {
            standby()
        }
    }
}