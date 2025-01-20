package collectorComponent

import element.ActivityExecutor
import org.kalasim.Component
import org.kalasim.misc.time.meanOf
import totalTime
import kotlin.time.Duration


class QueueSnapshotCollector(val snapshotTimeout: Duration) : Component("QueueSnapshotCollector") {
    class QueueMetric(val time: Long, val jobsInQueue: MutableList<String>) {
        fun removeAll(notTakenJobs: List<String>) {
            jobsInQueue.removeAll(notTakenJobs)
        }

        val count: Int get() = jobsInQueue.count()
    }

    val executorsMap: Map<String, List<ActivityExecutor>> = get()

    val queuesMetric: MutableMap<String, MutableList<QueueMetric>> = mutableMapOf()

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val snapshotTime = env.totalTime()

        executorsMap.forEach { _, executorsList ->
            executorsList.forEach { executor ->
                val queueMetric = QueueMetric(snapshotTime, executor.getJobsDetails() as MutableList<String>)

                queuesMetric[executor.id]?.let { it.forEach { it.removeAll(executor.notTakenJobs) } }
                queuesMetric.getOrPut(executor.id) { mutableListOf() }.add(queueMetric)
            }
        }

        hold(snapshotTimeout)
    }

    val means: Map<String, Double> get() = queuesMetric.mapValues { it.value.map { it.count }.average() }
}