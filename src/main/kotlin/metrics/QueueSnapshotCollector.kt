package metrics

import org.jetbrains.kotlinx.dataframe.math.varianceAndMean
import org.kalasim.SimTime
import org.kalasim.misc.roundAny
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration

class QueueSnapshotCollector(val snapshotTimeout: Duration, private val timeZero: SimTime) {
    class Snapshot(val time: SimTime, var count: Int)

    val snapshots = mutableListOf<Snapshot>()
    private var max = 0

    fun jobTaken(startTime: SimTime, endTime: SimTime) {
        // Add missing snapshot from last Snapshot to endTime with count = 0. One each snapshotTimeout
        var lastSnapshotTime = snapshots.lastOrNull()?.time ?: timeZero
        while (lastSnapshotTime.plus(snapshotTimeout) <= endTime) {
            lastSnapshotTime = lastSnapshotTime.plus(snapshotTimeout)
            snapshots.add(Snapshot(lastSnapshotTime, 0))
        }

        // Increment snapshot's count of when snapshot's time is between startTime and endTime
        snapshots.filter { it.time in startTime..endTime }.forEach {
            it.count++

            if (it.count > max) max = it.count
        }
    }

    val metrics: Map<String, Number>
        get() {
            val metric = snapshots.map { it.count }.varianceAndMean()

            return mapOf(
                "avg" to BigDecimal(metric.mean.takeIf { it.isFinite() } ?: 0.0).setScale(2, RoundingMode.FLOOR).toDouble(),
                "variance" to BigDecimal(metric.variance.takeIf { it.isFinite() } ?: 0.0).setScale(2, RoundingMode.FLOOR).toDouble(),
                "std" to BigDecimal(metric.std(0).takeIf { it.isFinite() } ?: 0.0).setScale(2, RoundingMode.FLOOR).toDouble(), // TODO: understand which degree of freedom
                "max" to max,
                "min" to 0
            )
        }
}