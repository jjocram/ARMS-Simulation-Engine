package collectorComponent

import element.ActivityExecutor
import org.jetbrains.kotlinx.dataframe.math.std
import org.kalasim.Component
import totalTime
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration


class QueueSnapshotCollector(val snapshotTimeout: Duration) : Component("QueueSnapshotCollector") {
    class QueueMetric(val time: Long, val jobsInQueue: MutableList<String>) {
        fun removeAll(notTakenJobs: List<String>) {
            jobsInQueue.removeAll(notTakenJobs)
        }

        val count: Int get() = jobsInQueue.count()
    }

    private val executorsMap: Map<String, List<ActivityExecutor>> = get()

    private val queuesMetric: MutableMap<String, MutableList<QueueMetric>> = mutableMapOf()

    private var cleanUpDone = false

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val snapshotTime = env.totalTime()

        executorsMap.forEach { _, executorsList ->
            executorsList.forEach { executor ->
                val queueMetric = QueueMetric(snapshotTime, executor.getJobsDetails() as MutableList<String>)

                queuesMetric.getOrPut(executor.id) { mutableListOf() }.add(queueMetric)
            }
        }

        hold(snapshotTimeout)
    }

    private fun cleanUp() {
        if (!cleanUpDone) {
            cleanUpDone = true
            executorsMap.forEach { _, executorsList ->
                executorsList.forEach { executor ->
                    queuesMetric[executor.id]?.forEach { it.removeAll(executor.notTakenJobs) }
                }
            }
        }
    }

    val means: Map<String, Double>
        get() {
            cleanUp()
            return queuesMetric.mapValues { it.value.map { it.count }.average() }
        }

    val variances: Map<String, Double>
        get() {
            cleanUp()
            return queuesMetric.mapValues { (executor, metrics) ->
                metrics.map { it.count }.map { (it - means[executor]!!).pow(2) }.average()
            }
        }

    val stds: Map<String, Double>
        get() {
            cleanUp()
            return queuesMetric.mapValues { sqrt(variances[it.key]!!) }
        }

    val metrics: Map<String, Map<String, Double>>
        get() = queuesMetric.mapValues {
            mapOf(
                "mean" to means[it.key]!!,
                "variance" to variances[it.key]!!,
                "std" to stds[it.key]!!
            )
        }
}