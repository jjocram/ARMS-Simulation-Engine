package element

import metrics.QueueSnapshotCollector
import metrics.TimeDeltaMetric
import org.kalasim.Component
import place.Place
import token.ControlToken
import token.ProductToken
import token.ResourceToken
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

fun maxMultiplier(available: List<Int>, required: List<Int>): Int {
    return available.zip(required).minOfOrNull { (a, r) -> if (r == 0) Int.MAX_VALUE else a / r } ?: 0
}

class Job(
    val resourcesToTake: List<ResourceRequest>,
    val resourceToProduce: List<ResourceRequest>,
    val controlToken: ControlToken,
    val productToken: ProductToken,
    val outputControl: Place,
    val outputProduct: Place,
    val activityId: String,
) {
    val id: String = UUID.randomUUID().toString()
    var taken = false
    val allResourcesAvailable: Boolean get() = resourcesToTake.all { it.isEnough }
    fun allResourcesAvailableWithBatch(batchSize: Int): Boolean =
        resourcesToTake.map { ResourceRequest(it.resource, it.quantity * batchSize) }.all { it.isEnough }

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
    class ExecutorJob(
        val job: Job,
        val compatibility: Compatibility
    ) {
        val resourcesToTake get() = job.resourcesToTake
        val resourceToProduce get() = job.resourceToProduce
        val allAccessoriesAvailable: Boolean get() = compatibility.accessories.all { it.isEnough }
        fun allResourcesAvailableWithBatch(batchSize: Int): Boolean = job.allResourcesAvailableWithBatch(batchSize)
        val isTaken: Boolean get() = job.taken
        fun take() {
            require(job.taken == false) { "Job is already taken" }
            job.taken = true
        }

        fun moveTokens() = job.moveTokens()
    }

    /**
     * Maps how much time a job spent in the queue. Grouped by the activity which assigned the job
     */
    val jobsInQueueMetrics = mutableMapOf<String, TimeDeltaMetric>()

    /**
     * Maps how much time this executor spent for an activity
     */
    val timeByActivities = mutableMapOf<String, TimeDeltaMetric>()

    val queueLengthMetric = QueueSnapshotCollector(Duration.parse("5s"), env.startDate)


    private val jobs = mutableListOf<ExecutorJob>()
    private val jobsMaxBatchSizeAttempts = mutableMapOf<Compatibility, Boolean>()

    fun addJob(job: Job, compatibility: Compatibility) {
        jobs.add(ExecutorJob(job, compatibility))

        val metricToSet = jobsInQueueMetrics.getOrDefault(job.activityId, TimeDeltaMetric())
        metricToSet.add(job, env.now)
        jobsInQueueMetrics[job.activityId] = metricToSet
    }

    val countJobs: Int get() = jobs.count()

    override fun repeatedProcess(): Sequence<Component> = sequence {
        jobs.removeAll { it.isTaken }
        while (jobs.isEmpty()) passivate()

        // Get the first job in the queue. This will be the reference in case of batch
        val firstJob = jobs.firstOrNull { !it.isTaken } // Do not remove the first job from the list of jobs
        if (firstJob == null) return@sequence

        // If this kind of job has NOT already tried working on this machine and there is not enough batch. Then, move
        // it to the end of the jobs queue and restart the process
        val hasAlreadyTriedMaxBatch = jobsMaxBatchSizeAttempts.getOrDefault(firstJob.compatibility, false)
        if (hasAlreadyTriedMaxBatch.not() &&
            firstJob.allResourcesAvailableWithBatch(firstJob.compatibility.batchSize).not()
        ) {
            jobs.add(jobs.removeAt(0))
            standby()
            return@sequence
        }

        // If not all accessories are available, then move the job to the end of the jobs queue and restart the process
        if (firstJob.allAccessoriesAvailable.not()) {
            // Not all accessories requires are available. Move the job later in the queue of jobs and restart
            jobs.add(jobs.removeAt(0))
            standby()
            return@sequence
        }

        // Compute the batch size: if can take max then take it else take the maximum amount possible
        val batchSize = if (firstJob.allResourcesAvailableWithBatch(firstJob.compatibility.batchSize)) {
            firstJob.compatibility.batchSize
        } else {
            maxMultiplier(
                firstJob.resourcesToTake.map { it.resource.count() },
                firstJob.resourcesToTake.map { it.quantity })
        }

        // Get the list of jobs to work on
        val batchedJobs = jobs
            .filter { !it.isTaken }
            .filter { it.compatibility == firstJob.compatibility }
            .take(batchSize)

        // Take each job and save the data for analytics
        batchedJobs.forEach { job ->
            job.take()
            val jobTimes = jobsInQueueMetrics.getValue(job.job.activityId).complete(job.job, env.now)
            timeByActivities.getOrPut(job.job.activityId) { TimeDeltaMetric() }.add(job.job, env.now)
            queueLengthMetric.jobTaken(jobTimes.start, jobTimes.end!!)
        }

        // Take the accessories
        val accessoryTokens =
            firstJob.compatibility.accessories.map { Pair(it.resource, it.resource.take(it.quantity)) }

        // Take the resources
        firstJob.resourcesToTake.forEach { it.resource.take(it.quantity * batchSize) }

        // Wait for the executor to complete the process
        hold(firstJob.compatibility.duration)

        // Assign affinity
        batchedJobs.forEach { job ->
            job.job.productToken.setExecutorAffinity(job.job.activityId, this@ActivityExecutor.id)
        }

        // Add tokens to outputs
        batchedJobs.forEach { job -> job.moveTokens() }

        // Put back accessory tokens
        accessoryTokens.forEach { (place, tokens) -> place.add(tokens) }

        // Create resourceToProduce tokens
        firstJob.resourceToProduce.forEach {
            val tokens = List(it.quantity * batchSize) {
                val token = ResourceToken()
                token.push(UUID.randomUUID())
                token
            }
            it.resource.add(tokens)
        }

        // Save time took to work on these jobs
        batchedJobs.forEach { job -> timeByActivities.getValue(job.job.activityId).complete(job.job, env.now) }

        // Wait a very small amount of time to let some other executors that has to use the same accessories to start working. If none is ready then just a very small amount of time is lost
        // This is a workaround. A proper implementation would require checking if any other executioner can be ready and randomly (or algorithmic) select one.
        // This breaks the eager mechanism of "I work until I have jobs to do" to a more fair solution (fair > eager)
        hold(1.microseconds)
    }
}