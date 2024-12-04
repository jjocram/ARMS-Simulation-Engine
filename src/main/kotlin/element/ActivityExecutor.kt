package element

import metrics.TimeDeltaMetric
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
    val activityId: String
) {
    private val id: String = UUID.randomUUID().toString()
    var taken = false
    val allResourcesAvailable: Boolean get() = resourcesToTake.all { it.isEnough }

    fun moveTokens() {
        outputControl.add(controlToken)
        outputProduct.add(productToken)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Job

        if (taken != other.taken) return false
        if (resourcesToTake != other.resourcesToTake) return false
        if (resourceToProduce != other.resourceToProduce) return false
        if (controlToken != other.controlToken) return false
        if (productToken != other.productToken) return false
        if (outputControl != other.outputControl) return false
        if (outputProduct != other.outputProduct) return false
        if (activityId != other.activityId) return false
        if (id != other.id) return false
        if (allResourcesAvailable != other.allResourcesAvailable) return false

        return true
    }
}

class ActivityExecutor(val id: String, name: String?) : Component(name ?: id) {
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

    val jobsInQueueMetrics: MutableMap<String, TimeDeltaMetric> = mutableMapOf()
    val timeByActivities = mutableMapOf<String, TimeDeltaMetric>()

    private val jobs = mutableListOf<ExecutorJob>()
    fun addJob(job: Job, duration: Duration, accessories: List<ResourceRequest>) {
        jobs.add(ExecutorJob(job, duration, accessories))

        val metricToSet = jobsInQueueMetrics.getOrDefault(job.activityId, TimeDeltaMetric())
        metricToSet.add(job, env.now)
        jobsInQueueMetrics[job.activityId] = metricToSet
    }

    val countJobs: Int get() = jobs.count()

    override fun repeatedProcess(): Sequence<Component> = sequence {
        jobs.removeAll { it.isTaken }
        while (jobs.isEmpty()) passivate()

        val job = jobs.firstOrNull { !it.isTaken }
        if (job != null) {
            if (job.allAccessoriesAvailable && job.allResourcesAvailable) { // All condition to start working are checked
                // This executor will work on this job
                job.take()
                jobsInQueueMetrics.getValue(job.job.activityId).complete(job.job, env.now)
                timeByActivities.getOrPut(job.job.activityId) { TimeDeltaMetric() }.add(job.job, env.now)

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

                timeByActivities.getValue(job.job.activityId).complete(job.job, env.now)
            } else {
                // Skip this job for now
                jobs.add(job)
                standby()
            }
        }
    }
}