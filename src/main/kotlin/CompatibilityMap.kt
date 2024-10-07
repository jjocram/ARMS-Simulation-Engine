import CompatibilityMap.CompatibilityItem.AccessoryCompatibility
import kotlin.time.Duration

class CompatibilityMap {
    class CompatibilityItem(
        val element: BPMNElement,
        val productFamily: ProductFamily,
        val executor: Executor,
        val duration: Duration,
        val batchSize: Int,
        val accessories: List<AccessoryCompatibility>,
    ) {
        class AccessoryCompatibility(val accessory: Accessory, val quantity: Int)
    }

    private val compatibilities: MutableList<CompatibilityItem> = mutableListOf()

    fun add(
        element: BPMNElement,
        productFamily: ProductFamily,
        executor: Executor,
        duration: Duration,
        batchSize: Int,
        accessories: List<AccessoryCompatibility>,
    ) = compatibilities.add(CompatibilityItem(element, productFamily, executor, duration, batchSize, accessories))

    fun get(element: BPMNElement?, productFamily: ProductFamily?, executor: Executor?): List<CompatibilityItem> =
        get(element?.id, productFamily?.id, executor?.id)

    fun get(elementId: String?, productFamilyId: String?, executorId: String?): List<CompatibilityItem> {
        val isElement = elementId != null
        val isProductFamily = productFamilyId != null
        val isExecutor = executorId != null

        return compatibilities.filter {
            (if (isElement) it.element.id == elementId else true) &&
                    (if (isProductFamily) it.productFamily.id == productFamilyId else true) &&
                    (if (isExecutor) it.executor.id == executorId else true)
        }
    }
}