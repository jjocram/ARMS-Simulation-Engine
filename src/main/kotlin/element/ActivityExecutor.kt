package element

import org.kalasim.Component
import place.Place
import token.ControlToken
import token.ProductToken
import token.ResourceToken
import java.util.UUID
import kotlin.time.Duration

class Job(
    val resourcesToTake: List<ResourceRequest>,
    val resourceToProduce: List<ResourceRequest>,
    val controlToken: ControlToken,
    val productToken: ProductToken,
    val outputControl: Place,
    val outputProduct: Place,
) {
    var taken = false
    val allResourcesAvailable: Boolean get() = resourcesToTake.all { it.isEnough }

    fun moveTokens() {
        outputControl.add(controlToken)
        outputProduct.add(productToken)
    }
}

class ActivityExecutor(id: String, name: String?) : Component(name ?: id) {
    class ExecutorJob(val job: Job, val duration: Duration, val accessories: List<ResourceRequest>) {
        val resourcesToTake get() = job.resourcesToTake
        val resourceToProduce get() = job.resourceToProduce
        val allAccessoriesAvailable: Boolean get() = accessories.all { it.isEnough }
        val allResourcesAvailable: Boolean get() = job.allResourcesAvailable
        val isTaken: Boolean get() = job.taken
        fun take() {
            require(job.taken == false) { "Job is already taken" }
            job.taken = true
        }

        fun moveTokens() = job.moveTokens()
    }

    private val jobs = mutableListOf<ExecutorJob>()
    fun addJob(job: Job, duration: Duration, accessories: List<ResourceRequest>) =
        jobs.add(ExecutorJob(job, duration, accessories))

    val countJobs: Int get() = jobs.count()

    override fun repeatedProcess(): Sequence<Component> = sequence {
        jobs.removeAll { it.isTaken }
        while (jobs.isEmpty()) passivate()

        val job = jobs.firstOrNull { !it.isTaken }
        if (job != null) {
            if (job.allAccessoriesAvailable && job.allResourcesAvailable) { // All condition to start working are checked
                // This executor will work on this job
                job.take()

                // Take accessory tokens
                val accessoryTokens = job.accessories.map { Pair(it.resource, it.resource.take(it.quantity)) }

                // Take resourceToTake tokens
                job.resourcesToTake.forEach { it.resource.take(it.quantity) }

                // Hold time
                hold(job.duration)

                // Add tokens to outputs
                job.moveTokens()

                // Put back accessory tokens
                accessoryTokens.forEach { (place, tokens) -> place.add(tokens) }

                // Create resourceToProduce tokens
                job.resourceToProduce.forEach {
                    val tokens = List(it.quantity) {
                        val token = ResourceToken()
                        token.push(UUID.randomUUID())
                        token
                    }
                    it.resource.add(tokens)
                }
            } else {
                // Skip this job for now
                jobs.add(job)
                standby()
            }
        }
    }
}