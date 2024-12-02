package metrics

import element.Job
import org.kalasim.SimTime
import kotlin.time.Duration

class TimeDeltaMetric {
    private class TimeDelta(val start: SimTime, var end: SimTime?) {
        val delta: Duration
            get() {
                require(end != null) { "Job delta without end" }
                return end!! - start
            }
    }

    private val jobsDelta: MutableMap<Job, TimeDelta> = mutableMapOf()

    fun add(job: Job, time: SimTime) = jobsDelta.set(job, TimeDelta(time, null))
    fun complete(job: Job, time: SimTime) {
        require(jobsDelta.containsKey(job)) { "Job (${job}) not found" }
        jobsDelta.getValue(job).end = time
    }

    val max: Long get() = jobsDelta.values.filter{ it.end != null }.maxOfOrNull { it.delta.inWholeSeconds / 60 } ?: -1
    val min: Long get() = jobsDelta.values.filter{ it.end != null }.minOfOrNull { it.delta.inWholeSeconds / 60 } ?: -1
    val avg: Double get() = jobsDelta.values.filter{ it.end != null }.map { it.delta.inWholeSeconds / 60 }.average()
}