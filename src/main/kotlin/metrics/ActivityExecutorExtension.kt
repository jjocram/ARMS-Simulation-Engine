package metrics

import element.ActivityExecutor
import org.kalasim.ComponentState
import org.kalasim.toLifeCycleRecord
import kotlin.time.Duration
import kotlin.time.DurationUnit

enum class Metric {
    MAX, MIN, MEAN, SUM, COUNT
}

val ActivityExecutor.waitTimeInQueue: Map<Metric, Number>
    get() = if (jobsInQueueMetrics.values.isEmpty()) {
        mapOf(
            Metric.MAX to 0L,
            Metric.MIN to 0.0,
            Metric.MEAN to 0.0
        )
    } else {
        mapOf(
            Metric.MAX to jobsInQueueMetrics.values.maxOf { metric -> metric.max },
            Metric.MIN to jobsInQueueMetrics.values.minOf { metric -> metric.min },
            Metric.MEAN to jobsInQueueMetrics.values.map { it.mean }.average()
        )
    }

val ActivityExecutor.processedItems: Int
    get() = timeByActivities.map { it.value.count }.sum()

val ActivityExecutor.totalIdleTime: Double
    get() = toLifeCycleRecord().inPassive.toDouble(DurationUnit.MINUTES)

val ActivityExecutor.totalBusyTime: Double
    get() = toLifeCycleRecord().inScheduled.toDouble(DurationUnit.MINUTES)

val ActivityExecutor.metricsByActivity: Map<String, Map<String, TimeDeltaMetric>>
    get() = timeByActivities.mapValues { (key, value) ->
        mapOf(
            "busy" to value,
            "queue" to jobsInQueueMetrics.getValue(key)
        )
    }