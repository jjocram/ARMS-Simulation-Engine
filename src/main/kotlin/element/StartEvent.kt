package element

import org.kalasim.Component
import place.Place
import token.ControlToken
import token.ProductToken
import transition.Transition
import java.util.UUID

class ProductRequest(val productProperties: Map<String, String>, val quantity: Int)

class StartEvent(
    id: String,
    value: String?,
    outputControl: Place,
    outputProduct: Place,
    val productRequests: List<ProductRequest>
) :
    BPMNElement(id, value) {
    val transition = Transition(value ?: id) { return@Transition null }

    init {
        transition.addPlace("control", outputControl)
        transition.addPlace("product", outputProduct)
    }

    override fun process(): Sequence<Component> = sequence {
        for (product in productRequests) {
            val ids = List(product.quantity) { UUID.randomUUID() }
            val controlTokens = ids.map {
                val token = ControlToken()
                token.push(it)
                return@map token
            }
            val productTokens = ids.map {
                val token = ProductToken()
                token.push(it)
                product.productProperties.forEach { k, v -> token.setProperty(k, v) }
                return@map token
            }

            controlTokens.forEach { transition.getPlace("control").add(it) }
            productTokens.forEach { transition.getPlace("product").add(it) }
        }
    }
}