import org.kalasim.Component
import org.koin.core.component.inject

open class Executor(val id: String): Component(id) {
    val compatibilityMap: CompatibilityMap by inject()
}