package collectorComponent

import element.ActivityExecutor
import org.kalasim.Component
import totalTime
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
}