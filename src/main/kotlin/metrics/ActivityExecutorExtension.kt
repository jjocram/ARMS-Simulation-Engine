package metrics

import element.ActivityExecutor
import org.kalasim.ComponentState

enum class Metric {
    MAX, MIN, MEAN, SUM, COUNT
}

val ActivityExecutor.waitTimeInQueue: Map<Metric, Number>
    get() = mapOf(
        Metric.MAX to jobsInQueueMetrics.values.maxOf { metric -> metric.max },
        Metric.MIN to jobsInQueueMetrics.values.minOf { metric -> metric.min },
        Metric.MEAN to jobsInQueueMetrics.values.map { it.mean }.average()
    )

val ActivityExecutor.totalIdleTime: Double
    get() = stateTimeline.summed().getValue(ComponentState.PASSIVE)

val ActivityExecutor.totalBusyTime: Double
    get() = stateTimeline.summed().getValue(ComponentState.SCHEDULED)

val ActivityExecutor.metricsByActivity: Map<String, Map<String, TimeDeltaMetric>>
    get() = timeByActivities.mapValues { (key, value) ->
        mapOf(
            "busy" to value,
            "queue" to jobsInQueueMetrics.getValue(key)
        )
    }