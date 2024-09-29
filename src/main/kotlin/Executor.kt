import org.kalasim.Component
import org.koin.core.component.inject

open class Executor(val id: String): Component(id) {
    val compatibilityMap: CompatibilityMap by inject()

    fun wakeUpNextElementsOf(element: BPMNElement) {
        element.nextElements
            .flatMap {
                compatibilityMap.get(it, null, null) }
            .filter {
                it.executor.isPassive
            }
            .forEach {
                it.executor.activate()
            }
    }
}