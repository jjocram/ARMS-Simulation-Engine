package place

import token.Token
import java.util.UUID

class Place {
    val tokens: MutableList<Token> = mutableListOf()

    fun add(token: Token) = tokens.add(token)
    fun add(tokens: List<Token>) = this.tokens.addAll(tokens)

    fun take(id: List<UUID>): Token = tokens.removeAt(tokens.indexOfFirst { it.ids == id })
    fun take(): Token = tokens.removeAt(0)
    fun take(n: Int): List<Token> {
        require(tokens.count() >= n) { "There are not enough tokens in place (request ${n}, found ${count()})" }
        return (0 until n).map { tokens.removeAt(0) }
    }

    fun count(): Int = tokens.count()
    fun isNotEmpty(): Boolean = tokens.isNotEmpty()
}