package webService

import kotlinx.serialization.*

@Serializable
data class SimulationResult(val totalTime: Long)

@Serializable
data class ExecutorActivityResult(
    val id: String,
    val maxWaitTimeInQueue: Long,
    val avgWaitTimeInQueue: Double,
    val sumWaitTimeInQueue: Long,
    val busy: Long,
    val processedItems: Int,
)

@Serializable
data class ExecutorResult(
    val id: String,
    val maxWaitTimeInQueue: Long,
    val avgWaitTimeInQueue: Double,
    val sumWaitTimeInQueue: Long,
    val busy: Double,
    val idle: Double,
    val processedItems: Int,
    val avgQueueLength: Double,
    val varQueueLength: Double,
    val stdQueueLength: Double,
    val maxQueueLength: Int,
    val activities: List<ExecutorActivityResult>
)

@Serializable
data class MetricResult(
    val simulation: SimulationResult,
    val executors: List<ExecutorResult>
)