package collectorComponent

import element.ActivityExecutor
import org.kalasim.Component
import totalTime
import kotlin.time.Duration


class QueueSnapshotCollector(name: String, val snapshotTimeout: Duration) : Component(name) {
    class QueueMetric(val time: Long)

    val executorsMap: Map<String, List<ActivityExecutor>> = get()

    val queuesMetric: Map<String, MutableList<QueueMetric>> = mutableMapOf()

    override fun repeatedProcess(): Sequence<Component> = sequence {
        val snapshotTime = env.totalTime()

        executorsMap.forEach { _, executorsList ->
            executorsList.forEach { executor ->
                val queueMetric = QueueMetric(snapshotTime)

                queuesMetric.getOrDefault(executor.id, mutableListOf()).add(queueMetric)
            }
        }

        hold(snapshotTimeout)
    }
}