package webService

import kotlinx.serialization.Serializable

@Serializable
data class SimulationError(val message: String)