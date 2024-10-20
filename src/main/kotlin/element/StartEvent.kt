package element

import org.kalasim.Component
import place.Place
import token.ControlToken
import token.ProductToken
import transition.Transition
import java.util.UUID

class StartEvent(
    id: String,
    value: String?,
    inputControl: Place,
    outputControl: Place,
    inputProduct: Place,
    outputProduct: Place,
    val totalProducts: Int
) :
    BPMNElement(id, value) {
    val transition = Transition(value ?: id) { return@Transition true }

    init {
        transition.setControlPlaces(inputControl, outputControl) // Position 0 is for control
        transition.setProductPlaces(inputProduct, outputProduct) // Position 1 is for product
    }

    override fun process(): Sequence<Component> = sequence {
        val ids = List(totalProducts) { UUID.randomUUID() }
        val controlTokens = ids.map {
            val token = ControlToken()
            token.push(it)
            return@map token
        }
        val productTokens = ids.map {
            val token = ProductToken()
            token.push(it)
            return@map token
        }

        transition.connections[0].output.tokens.addAll(controlTokens) // Position 0 is for control
        transition.connections[1].output.tokens.addAll(productTokens) // Position 1 is for product
    }
}