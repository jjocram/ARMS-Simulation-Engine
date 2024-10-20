package transition

import place.Place
import token.Token

class Transition(val name: String, val canFirePredicate: (Transition) -> Boolean) {
    class IO(val input: Place, val output: Place) {
        fun move(token: Token) {
            val tokenToMove = input.take(token)
            output.add(tokenToMove)
        }

        fun take(token: Token) = input.take(token)
        fun take(index: Int) = input.take(index)

        fun put(token: Token) = output.add(token)
    }

    class TransientToken(private val token: Token, private val io: IO) {
        fun move() = io.put(token)
    }

    val connections = mutableListOf<IO>()

    fun setControlPlaces(input: Place, output: Place) {
        connections.add(0, IO(input, output))
    }

    fun setProductPlaces(input: Place, output: Place) {
        connections.add(1, IO(input, output))
    }

    val canFire: Boolean get() = canFirePredicate(this)

    fun addConnection(input: Place, output: Place) = connections.add(IO(input, output))

    fun fireImmediately() {
        moveTransientTokens(takeInputTransientTokens())
    }

    fun takeInputTransientTokens(): List<TransientToken> {
        val transientTokens = mutableListOf<TransientToken>()
        var tokensInControlInput = connections[0].input.tokens.count()
        for (i in 0 until tokensInControlInput) { // Iterate over tokens in the controlInput place (always at position 0)
            val controlToken = connections[0].take(i)
            // Each control token has to be also in the product (first position) places
            if (connections[1].input.has(controlToken)) {
                // Take the controlToken and the product token associated
                transientTokens.add(TransientToken(controlToken, connections[0]))
                transientTokens.add(TransientToken(connections[1].take(controlToken), connections[1]))

                // Take only the first token that match
                break
            } else {
                // Re-add the control token removed to check
                connections[0].put(controlToken)
            }
        }
        return transientTokens
    }

    fun moveTransientTokens(tokens: List<TransientToken>) {
        tokens.forEach { it.move() }
    }
}