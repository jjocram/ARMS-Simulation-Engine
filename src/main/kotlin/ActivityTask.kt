import org.kalasim.Component
import org.koin.core.component.inject

class Activity(
    override val id: String,
    override var nextElements: List<BPMNElement>,
    val transformations: TransformationMap
) : BPMNElement {
    override val activationTokens: MutableList<ActivationToken> = mutableListOf()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is Activity && id == other.id


}

class ActivityExecutor(executorId: String) : Executor(executorId) {
    private val inventories: Inventories by inject()

    override fun process(): Sequence<Component> = sequence {
        val compatibilities = compatibilityMap.get(null, null, this@ActivityExecutor)
            .groupBy { it.element as Activity }
        require(compatibilities.isNotEmpty()) { "ActivityExecutor $id must have at least one compatibility associated" }

        while (true) {
            var worked = false

            for ((activity, compatibilitiesInActivity) in compatibilities) {
                for (compatibility in compatibilitiesInActivity) {
                    activity.activationTokens.firstOrNull { it.productFamily == compatibility.productFamily }
                        ?.let { token ->

                            val inputResourceRequest =
                                activity.transformations.get(compatibility.productFamily).inputs.map {
                                    Pair(inventories.get(it.productFamily), it.quantity * compatibility.batchSize)
                                }
                            val isEnoughInputResources =
                                inputResourceRequest.count { it.first.level >= it.second } == inputResourceRequest.count()

                            if (isEnoughInputResources) {
                                worked = true

                                // Remove activation token
                                activity.activationTokens.remove(token)

                                // Take input resources necessary to perform the work
                                inputResourceRequest.forEach { (resource, quantity) -> take(resource, quantity) }

                                // Wait time
                                hold(compatibility.duration)

                                // Create new products
                                activity.transformations.get(compatibility.productFamily).outputs
                                    .forEach { put(inventories.get(it.productFamily), it.quantity) }

                                // Add activation token to next elements
                                activity.nextElements.forEach { it.activationTokens.add(token) }
                            }
                        }
                }
            }

            if (!worked) {
                standby()
            }
        }
    }
}