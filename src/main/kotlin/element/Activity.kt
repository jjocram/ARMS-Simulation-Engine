package element

import org.kalasim.Component
import place.Place
import token.ControlToken
import token.ProductToken
import kotlin.time.Duration

class ResourceRequest(val resource: Place, val quantity: Int) {
    val isEnough: Boolean get() = resource.count() >= quantity
}

class Compatibility(
    val executor: ActivityExecutor,
    val productProperties: Map<String, String>,
    val accessories: List<ResourceRequest>,
    val duration: Duration,
)

class Transformation(
    val resourcesToTake: List<ResourceRequest>,
    val resourcesToProduce: List<ResourceRequest>,
    val productProperties: Map<String, String>,
    val transformationToApply: Map<String, String>
)

class Activity(
    val id: String,
    value: String,
    val inputs: List<Pair<Place, Place>>,
    val outputControl: Place,
    val outputProduct: Place,
    val compatibilities: List<Compatibility>,
    val transformations: List<Transformation>,
) : BPMNElement(id, value) {
    override fun repeatedProcess(): Sequence<Component> = sequence {
        while (inputs.any { (control, product) -> control.isNotEmpty() && product.isNotEmpty() }) {
            val (inputControl, inputProduct) = inputs
                .first { (control, product) -> control.isNotEmpty() && product.isNotEmpty() }

            // Take token from inputs
            val controlInputIds = inputControl.tokens.map { it.ids }.toSet()
            val productInputIds = inputProduct.tokens.map { it.ids }.toSet()
            val tokenId = controlInputIds.intersect(productInputIds).firstOrNull()
            require(tokenId != null) { "Token mismatch in $name for $controlInputIds and $productInputIds" }

            val controlToken = inputControl.take(tokenId) as ControlToken
            val productToken = inputProduct.take(tokenId) as ProductToken

            // Find compatibility
            val compatibilities = compatibilities
                .filter { it.productProperties.all { (k, v) -> v == productToken.getProperty(k) } }
                .sortedBy { it.executor.countJobs } //TODO: add parameter to choose: isPassive?, queue length?
//                .first()

            // Find transformation
            val transformation =
                transformations.firstOrNull { it.productProperties.all { (k, v) -> v == productToken.getProperty(k) } }

            // Apply transformation to productToken
            productToken.apply(transformation?.transformationToApply ?: emptyMap())

            // Create new Job
            val job = Job(
                transformation?.resourcesToTake ?: emptyList(),
                transformation?.resourcesToProduce ?: emptyList(),
                controlToken,
                productToken,
                outputControl,
                outputProduct,
                id
            )
            compatibilities.forEach { compatibility ->
                compatibility.executor.addJob(job, compatibility.duration, compatibility.accessories)
            }

            // Activate all passive executors
            compatibilities.filter { it.executor.isPassive }.forEach { it.executor.activate() }
        }
        standby()
    }
}