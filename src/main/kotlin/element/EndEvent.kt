package element

import org.kalasim.Component
import place.Place
import token.ProductToken
import transition.Transition

class EndEvent(
    id: String,
    value: String?,
    inputControl: Place,
    outputControl: Place,
    inputProduct: Place,
    outputProduct: Place
) : BPMNElement(id, value) {
    private val transition = Transition(value ?: id) {
        val controlInputIds = it.getPlace("inputControl").tokens.map { it.ids }.toSet()
        val productInputIds = it.getPlace("inputProduct").tokens.map { it.ids }.toSet()
        return@Transition controlInputIds.intersect(productInputIds).firstOrNull()
    }

    init {
        transition.addPlace("inputControl", inputControl)
        transition.addPlace("outputControl", outputControl)
        transition.addPlace("inputProduct", inputProduct)
        transition.addPlace("outputProduct", outputProduct)
    }

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val tokenId = transition.fireableTokenId
        if (tokenId != null) { // There is at least one token with the same id in both controlInputPlace and productInputPlace
            // Move from control input to control output
            val controlToken = transition.getPlace("inputControl").take(tokenId)
            transition.getPlace("outputControl").add(controlToken)

            // Move from product input to product output
            val productToken = transition.getPlace("inputProduct").take(tokenId)
            transition.getPlace("outputProduct").add(productToken)
            (productToken as ProductToken).productProperties.forEach { k, v -> log("${productToken.ids}->$k: $v") }
        } else {
            standby()
        }
    }
}