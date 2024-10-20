package place

import token.Token

class Place {
    val tokens: MutableList<Token> = mutableListOf()

    val isEmpty: Boolean get() = tokens.isEmpty()
    val isNotEmpty: Boolean get() = tokens.isNotEmpty()

    fun add(token: Token) = tokens.add(token)
    fun add(tokens: List<Token>) = this.tokens.addAll(tokens)

    fun take(token: Token): Token {
        val tokenIndex = tokens.indexOfFirst { it.ids == token.ids }
        return take(tokenIndex)
    }

    fun take(index: Int): Token {
        return tokens.removeAt(index)
    }

    fun has(token: Token): Boolean = tokens.find { it.ids == token.ids } != null
}