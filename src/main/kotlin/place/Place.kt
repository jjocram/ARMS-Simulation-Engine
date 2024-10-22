package place

import token.Token
import java.util.UUID

class Place {
    val tokens: MutableList<Token> = mutableListOf()

    fun add(token: Token) = tokens.add(token)
    fun add(tokens: List<Token>) = this.tokens.addAll(tokens)

    fun take(index: Int): Token = tokens.removeAt(index)

    fun take(id: List<UUID>): Token{
        val tokenIndex = tokens.indexOfFirst { it.ids == id }
        return take(tokenIndex)
    }
}