class EndEvent(override val id: String) : BPMNElement {
    override var nextElements: List<BPMNElement> = emptyList()
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is EndEvent && id == other.id
}

