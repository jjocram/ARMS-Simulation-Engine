import element.ExclusiveSplitCondition
import element.ProductRequest
import org.kalasim.day
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import place.Place
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.log
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun getElementsFrom(nodeList: NodeList): List<Element> {
    val elementList = mutableListOf<Element>()

    for (i in 0 until nodeList.length) {
        val node = nodeList.item(i)
        if (node.nodeType == Document.ELEMENT_NODE) {
            elementList.add(node as Element)
        }
    }

    return elementList
}

class XMLProductProperty(
    val key: String,
    val value: String
) {
    companion object {
        fun fromElement(element: Element): XMLProductProperty {
            return XMLProductProperty(
                element.getAttribute("key"),
                element.getAttribute("value")
            )
        }
    }
}

class XMLCompatibility(
    val id: String,
    val time: String,
    val timeUnit: String,
    val idActivity: String,
    val idExecutor: String,
    val productProperties: List<XMLProductProperty>,
    val accessories: List<XMLAccessoryCompatibility>,
    val batchSize: Int
) {
    class XMLAccessoryCompatibility(val id: String, val quantity: Int) {
        companion object {
            fun fromElement(element: Element): XMLAccessoryCompatibility {
                return XMLAccessoryCompatibility(
                    element.getAttribute("id"),
                    element.getAttribute("quantity").toInt()
                )
            }
        }
    }

    companion object {
        fun fromElement(element: Element): XMLCompatibility {
            return XMLCompatibility(
                element.getAttribute("id"),
                element.getAttribute("time"),
                element.getAttribute("timeUnit"),
                element.getAttribute("idActivity"),
                element.getAttribute("idExecutor"),
                getElementsFrom(element.childNodes)
                    .filter { it.nodeName == XMLProcess.PRODUCT_PROPERTY_LABEL }
                    .map { XMLProductProperty.fromElement(it) },
                getElementsFrom(element.childNodes)
                    .filter { it.nodeName == XMLProcess.ACCESSORY_COMPATIBILITY_LABEL }
                    .map { XMLAccessoryCompatibility.fromElement(it) },
                element.getAttribute("batch").toInt()
            )
        }
    }

    val duration: Duration
        get() {
            return when (timeUnit) {
                "s" -> time.toDouble().seconds
                "m" -> time.toDouble().minutes
                "h" -> time.toDouble().hours
                "d" -> time.toDouble().days
                else -> throw IllegalArgumentException("Unknown time unit $timeUnit")
            }
        }
}

class XMLTransformation(
    val id: String,
    val idActivity: String,
    val productProperties: List<XMLProductProperty>,
    val transformationToApply: List<XMLProductProperty>,
    val inputs: List<XMLTransformationIO>,
    val outputs: List<XMLTransformationIO>
) {
    class XMLTransformationIO(val id: String, val inventoryId: String, val quantity: Int) {
        companion object {
            fun fromElement(element: Element): XMLTransformationIO {
                return XMLTransformationIO(
                    element.getAttribute("id"),
                    element.getAttribute("inventoryId"),
                    element.getAttribute("quantity").toInt(),
                )
            }
        }
    }

    companion object {
        fun fromElement(element: Element): XMLTransformation {
            return XMLTransformation(
                element.getAttribute("id"),
                element.getAttribute("activityId"),
                getElementsFrom(element.childNodes)
                    .filter { it.nodeName == XMLProcess.PRODUCT_PROPERTY_TRANSFORMATION_LABEL }
                    .map { XMLProductProperty.fromElement(it) },
                getElementsFrom(element.childNodes)
                    .filter { it.nodeName == XMLProcess.TRANSFORMATION_TO_APPLY_LABEL }
                    .map { XMLProductProperty.fromElement(it) },
                getElementsFrom(element.childNodes)
                    .filter { it.nodeName == XMLProcess.TRANSFORMATION_INPUT_LABEL }
                    .map { XMLTransformationIO.fromElement(it) },
                getElementsFrom(element.childNodes)
                    .filter { it.nodeName == XMLProcess.TRANSFORMATION_OUTPUT_LABEL }
                    .map { XMLTransformationIO.fromElement(it) }
            )
        }
    }
}

class XMLSequenceFlow(val id: String, val sourceRef: String, val targetRef: String, val name: String?) {
    companion object {
        fun fromElement(element: Element): XMLSequenceFlow {
            return XMLSequenceFlow(
                element.getAttribute("id"),
                element.getAttribute("sourceRef"),
                element.getAttribute("targetRef"),
                element.getAttribute("name").ifBlank { null }
            )
        }
    }

    fun toCondition(places: Map<String, Place>): ExclusiveSplitCondition {
        assert(name != null, {"Exclusive split has to have a value assigned"})
        val isDefault = name == "default"

        return if (isDefault) {
            ExclusiveSplitCondition(
                places.getValue(id),
                places.getValue(id+"_product"),
                isDefault,
                ""
            )
        } else {
            ExclusiveSplitCondition(
                places.getValue(id),
                places.getValue(id+"_product"),
                isDefault,
                "$name"
            )
        }
    }
}

class XMLAccessory(val id: String, val name: String, val quantity: Int) {
    companion object {
        fun fromElement(element: Element): XMLAccessory {
            return XMLAccessory(
                element.getAttribute("id"),
                element.getAttribute("name"),
                element.getAttribute("quantity").toInt()
            )
        }
    }
}

class XMLExecutor(val id: String, val name: String, val quantity: Int) {
    companion object {
        fun fromElement(element: Element): XMLExecutor {
            return XMLExecutor(
                element.getAttribute("id"),
                element.getAttribute("name"),
                element.getAttribute("quantity").toInt()
            )
        }
    }
}

class XMLInventory(val id: String, val name: String, val startQuantity: Int) {
    companion object {
        fun fromElement(element: Element): XMLInventory {
            return XMLInventory(
                element.getAttribute("id"),
                element.getAttribute("name"),
                element.getAttribute("startQuantity").toInt()
            )
        }
    }
}

sealed interface XMLElement {
    val id: String
    val outgoings: List<String>
    val incomings: List<String>
}

fun getListOf(type: String, fromElement: Element): List<String> {
    return getElementsFrom(fromElement.childNodes)
        .filter { it.nodeName == type }
        .map { it.textContent }
}

class XMLProductRequest(val id: String, val productProperties: List<XMLProductProperty>, val quantity: Int) {
    companion object {
        fun fromElement(element: Element): XMLProductRequest {
            return XMLProductRequest(
                element.getAttribute("id"),
                getElementsFrom(element.childNodes)
                    .filter { it.nodeName == XMLProcess.PRODUCT_PROPERTY_LABEL }
                    .map { XMLProductProperty.fromElement(it) },
                element.getAttribute("quantity").toInt()
            )
        }
    }

    fun toProductRequest(): ProductRequest {
        return ProductRequest(id, productProperties.associate { it.key to it.value }, quantity)
    }
}

class XMLStartEvent(
    override val id: String,
    override val outgoings: List<String>,
) : XMLElement {
    override val incomings = emptyList<String>()

    companion object {
        fun fromElement(element: Element): XMLStartEvent {
            return XMLStartEvent(
                element.getAttribute("id"),
                getListOf(XMLProcess.OUTGOING_LABEL, element),
            )
        }
    }
}

class XMLEndEvent(override val id: String, override val incomings: List<String>) : XMLElement {
    override val outgoings: List<String> = emptyList()

    companion object {
        fun fromElement(element: Element): XMLEndEvent {
            return XMLEndEvent(
                element.getAttribute("id"),
                getListOf(XMLProcess.INCOMING_LABEL, element)
            )
        }
    }
}

class XMLTask(
    override val id: String,
    override val outgoings: List<String>,
    override val incomings: List<String>,
    val name: String,
    val affinityWith: String?
) : XMLElement {

    companion object {
        fun fromElement(element: Element): XMLTask {
            val affinity = element.getAttribute("affinity").takeIf { it.isNotEmpty() && it != "null" }

            return XMLTask(
                element.getAttribute("id"),
                getListOf(XMLProcess.OUTGOING_LABEL, element),
                getListOf(XMLProcess.INCOMING_LABEL, element),
                element.getAttribute("name"),
                affinity,
            )
        }
    }
}

class XMLTimeEvent(
    override val id: String,
    override val outgoings: List<String>,
    override val incomings: List<String>,
    val value: String
) : XMLElement {
    companion object {
        fun fromElement(element: Element): XMLTimeEvent {
            return XMLTimeEvent(
                element.getAttribute("id"),
                getListOf(XMLProcess.OUTGOING_LABEL, element),
                getListOf(XMLProcess.INCOMING_LABEL, element),
                element.getAttribute("value")
            )
        }
    }

    val duration: Duration
        get() {
            return 7.day // TODO: update to value
        }
}


enum class GatewayType {
    FORK, JOIN
}

class XMLExclusiveGateway(
    override val id: String,
    override val outgoings: List<String>,
    override val incomings: List<String>,
    val name: String
) : XMLElement {
    companion object {
        fun fromElement(element: Element): XMLExclusiveGateway {
            return XMLExclusiveGateway(
                element.getAttribute("id"),
                getListOf(XMLProcess.OUTGOING_LABEL, element),
                getListOf(XMLProcess.INCOMING_LABEL, element),
                element.getAttribute("name")
            )
        }
    }

    val type: GatewayType
        get() {
            require((outgoings.size > 1 && incomings.size == 1) || (outgoings.size == 1 && incomings.size > 1)) { "Gateway $id with the wrong number of incoming/outgoings" }

            if (outgoings.size == 1) {
                return GatewayType.JOIN
            } else {
                return GatewayType.FORK
            }
        }
}

class XMLParallelGateway(
    override val id: String,
    override val outgoings: List<String>,
    override val incomings: List<String>,
) : XMLElement {
    companion object {
        fun fromElement(element: Element): XMLParallelGateway {
            return XMLParallelGateway(
                element.getAttribute("id"),
                getListOf(XMLProcess.OUTGOING_LABEL, element),
                getListOf(XMLProcess.INCOMING_LABEL, element)
            )
        }
    }

    val type: GatewayType
        get() {
            require((outgoings.size > 1 && incomings.size == 1) || (outgoings.size == 1 && incomings.size > 1)) { "Gateway $id with the wrong number of incoming/outgoings" }

            if (outgoings.size == 1) {
                return GatewayType.JOIN
            } else {
                return GatewayType.FORK
            }
        }
}

class XMLProcess(xmlFile: File) {
    companion object {
        const val TRANSFORMATION_INPUT_LABEL = "factory:transformationInput"
        const val TRANSFORMATION_OUTPUT_LABEL = "factory:transformationOutput"
        const val PROCESS_LABEL = "bpmn:process"
        const val INCOMING_LABEL = "bpmn:incoming"
        const val OUTGOING_LABEL = "bpmn:outgoing"
        const val EXTENSION_ELEMENTS_LABEL = "bpmn:extensionElements"
        const val TRANSFORMATION_LABEL = "factory:transformation"
        const val SEQUENCE_FLOW_LABEL = "bpmn:sequenceFlow"
        const val START_EVENT_LABEL = "bpmn:startEvent"
        const val END_EVENT_LABEL = "bpmn:endEvent"
        const val EXECUTOR_LABEL = "factory:executor"
        const val TASK_LABEL = "bpmn:task"
        const val BATCH_TASK_LABEL = "factory:batch"
        const val INTERMEDIATE_CATCH_EVENT_LABEL = "bpmn:intermediateCatchEvent"
        const val EXCLUSIVE_GATEWAY_LABEL = "bpmn:exclusiveGateway"
        const val PARALLEL_GATEWAY_LABEL = "bpmn:parallelGateway"
        const val ACCESSORY_LABEL = "factory:accessory"
        const val ACCESSORY_COMPATIBILITY_LABEL = "factory:accessoryCompatibility"
        const val COMPATIBILITY_LABEL = "factory:compatibility"
        const val PRODUCT_PROPERTY_LABEL = "factory:productProperty"
        const val PRODUCT_PROPERTY_TRANSFORMATION_LABEL = "factory:transformationProductProperty"
        const val TRANSFORMATION_TO_APPLY_LABEL = "factory:transformationProductPropertyToApply"
        const val INVENTORY_LABEL = "factory:inventory"
        const val PRODUCT_REQUEST_LABEL = "factory:productRequest"
    }

    private val document: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)

    init {
        document.normalize()
    }

    private val root = document.documentElement
    private val process = root.getElementsByTagName(PROCESS_LABEL).item(0) as Element

    val productRequests = getXMLProductRequest()
    private fun getXMLProductRequest(): Map<String, XMLProductRequest> {
        val extensionElements = process.getElementsByTagName(EXTENSION_ELEMENTS_LABEL).item(0).childNodes
        return getElementsFrom(extensionElements)
            .filter { it.nodeName == PRODUCT_REQUEST_LABEL }
            .map { XMLProductRequest.fromElement(it) }
            .associateBy { it.id }
    }

    val accessories = getXMLAccessories()
    private fun getXMLAccessories(): Map<String, XMLAccessory> {
        val extensionElements = process.getElementsByTagName(EXTENSION_ELEMENTS_LABEL).item(0).childNodes
        return getElementsFrom(extensionElements)
            .filter { it.nodeName == ACCESSORY_LABEL }
            .map { XMLAccessory.fromElement(it) }
            .associateBy { it.id }
    }

    val compatibilities = getXMLCompatibilities()
    private fun getXMLCompatibilities(): Map<String, XMLCompatibility> {
        val extensionElement = process.getElementsByTagName(EXTENSION_ELEMENTS_LABEL).item(0).childNodes
        return getElementsFrom(extensionElement)
            .filter { it.nodeName == COMPATIBILITY_LABEL }
            .map { XMLCompatibility.fromElement(it) }
            .associateBy { it.id }
    }

    val transformations = getXMLTransformations()
    private fun getXMLTransformations(): Map<String, XMLTransformation> {
        val extensionElements = process.getElementsByTagName(EXTENSION_ELEMENTS_LABEL).item(0).childNodes
        return getElementsFrom(extensionElements)
            .filter { it.nodeName == TRANSFORMATION_LABEL }
            .map { XMLTransformation.fromElement(it) }
            .associateBy { it.id }
    }

    val sequenceFlows = getXMLSequenceFlows()
    private fun getXMLSequenceFlows(): Map<String, XMLSequenceFlow> {
        val sequenceElements = process.getElementsByTagName(SEQUENCE_FLOW_LABEL)
        return getElementsFrom(sequenceElements)
            .map { XMLSequenceFlow.fromElement(it) }
            .associateBy { it.id }
    }

    val executors = getXMLExecutors()
    private fun getXMLExecutors(): Map<String, XMLExecutor> {
        val executorsElements = process.getElementsByTagName(EXECUTOR_LABEL)
        return getElementsFrom(executorsElements)
            .map { XMLExecutor.fromElement(it) }
            .associateBy { it.id }
    }

    val inventories = getXMLInventories()
    private fun getXMLInventories(): Map<String, XMLInventory> {
        val inventoryElements = process.getElementsByTagName(INVENTORY_LABEL)
        return getElementsFrom(inventoryElements)
            .map { XMLInventory.fromElement(it) }
            .associateBy { it.id }
    }

    private val tasks = getXMLTasks()
    private fun getXMLTasks(): Map<String, XMLTask> {
        val taskElements = process.getElementsByTagName(TASK_LABEL)
        val batchTaskElements = process.getElementsByTagName(BATCH_TASK_LABEL)
        return (getElementsFrom(taskElements) + getElementsFrom(batchTaskElements))
            .map { XMLTask.fromElement(it) }
            .associateBy { it.id }
    }

    private val timeEvents = getXMLTimeEvents()
    private fun getXMLTimeEvents(): Map<String, XMLTimeEvent> {
        val intermediateCatchEvents = process.getElementsByTagName(INTERMEDIATE_CATCH_EVENT_LABEL)
        fun isTimeEvent(element: Element): Boolean {
            return element.getElementsByTagName("bpmn:timerEventDefinition").length > 0
        }

        return getElementsFrom(intermediateCatchEvents)
            .filter { isTimeEvent(it) }
            .map { XMLTimeEvent.fromElement(it) }
            .associateBy { it.id }
    }

    private val exclusiveGateways = getXMLExclusiveGateways()
    private fun getXMLExclusiveGateways(): Map<String, XMLExclusiveGateway> {
        val exclusiveGateways = process.getElementsByTagName(EXCLUSIVE_GATEWAY_LABEL)
        return getElementsFrom(exclusiveGateways)
            .map { XMLExclusiveGateway.fromElement(it) }
            .associateBy { it.id }
    }

    private val parallelGateways = getXMLParallelGateways()
    private fun getXMLParallelGateways(): Map<String, XMLParallelGateway> {
        val parallelGateways = process.getElementsByTagName(PARALLEL_GATEWAY_LABEL)
        return getElementsFrom(parallelGateways)
            .map { XMLParallelGateway.fromElement(it) }
            .associateBy { it.id }
    }

    private val startEvent =
        XMLStartEvent.fromElement(process.getElementsByTagName(START_EVENT_LABEL).item(0) as Element)

    private val endEvents = getXMLEndEvents()
    private fun getXMLEndEvents(): Map<String, XMLEndEvent> {
        val endEventsElements = process.getElementsByTagName(END_EVENT_LABEL)
        return getElementsFrom(endEventsElements)
            .map { XMLEndEvent.fromElement(it) }
            .associateBy { it.id }
    }

    val xmlElements: Map<String, XMLElement> =
        mapOf(startEvent.id to startEvent) + parallelGateways + exclusiveGateways + timeEvents + tasks + endEvents
}