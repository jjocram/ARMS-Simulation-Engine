package token

import java.util.UUID

abstract class Token {
    val ids: MutableList<UUID> = mutableListOf()

    fun push(id: UUID) {
        ids.add(id)
    }

    fun pop() {
        ids.removeLast()
    }
}