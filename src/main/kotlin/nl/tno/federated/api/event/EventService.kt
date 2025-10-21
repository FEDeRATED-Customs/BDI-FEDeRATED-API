// FEDeRATED node
//
// Copyright (c) 2024-2025 Netherlands Organization for Applied Scientific Research TNO
//
// This file is part of the FEDeRATED Node API.
//
// This software is provided to Dutch Customs under the following terms:
//
// * Dutch Customs is granted a non-exclusive, non-transferable, and non-sublicensable license
//   to use, modify, and maintain the software for internal purposes only.
//
// * Dutch Customs is not permitted to disclose, distribute, or share the software, in whole or in
//   part, to any third party without prior written consent from TNO.
//
// * Any modifications made by Dutch Customs to the software must include this copyright and
//   license notice.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
// FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/**
 *
 */

package nl.tno.federated.api.event

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDestination
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDistributionService
import nl.tno.federated.api.event.mapper.EventMapper
import nl.tno.federated.api.event.mapper.UnsupportedEventTypeException
import nl.tno.federated.api.event.query.EventQuery
import nl.tno.federated.api.event.query.graphdb.GraphDBEventQueryService
import nl.tno.federated.api.event.type.EventType
import nl.tno.federated.api.event.type.EventTypeMapping
import nl.tno.federated.api.event.type.EventTypeMappingException
import nl.tno.federated.api.event.validation.JSONValidator
import nl.tno.federated.api.event.validation.ShaclValidator
import nl.tno.federated.api.graphdb.GraphDBService
import nl.tno.federated.api.orchestrator.OrchestratorService
import org.springframework.stereotype.Service
import java.util.*


const val EVENT_UUID_FIELD = "UUID"
const val EVENT_TYPE_FIELD = "eventType"

@Service
class EventService(
    private val eventMapper: EventMapper,
    private val eventQueryService: GraphDBEventQueryService,
    private val eventDistributionService: OrchestratorEventDistributionService,
    private val eventTypeMapping: EventTypeMapping,
    private val graphDBService: GraphDBService,
    private val orchestratorService: OrchestratorService

) {

    /**
     * Convert the given event to RDF.
     *
     * @throws UnsupportedEventTypeException is an unsupported Event type is encountered.
     */
    fun newJsonEvent(event: String, eventType: String, eventDestinations: Set<String>? = null): EnrichedEvent {
        val type = eventTypeMapping.getEventType(eventType) ?: throw EventTypeMappingException("EventType not found: $eventType")
        val enrichedEvent = enrichJsonEvent(event, type)
        validateWithShacl(enrichedEvent)

      //  publishRDFEvent(enrichedEvent, eventDestinations)
        graphDBService.insertEvent(enrichedEvent.eventRDF)
        return enrichedEvent
    }

    fun validateNewJsonEvent(event: String, eventType: String): EnrichedEvent {
        val type = eventTypeMapping.getEventType(eventType) ?: throw EventTypeMappingException("EventType not found: $eventType")
        val enrichedEvent = enrichJsonEvent(event, type)
        validateWithShacl(enrichedEvent)
        return enrichedEvent
    }

    fun findAll(page: Int, size: Int): List<JsonNode> {
        val result = orchestratorService.findAllMessages(page,size)
        return result.map {
            val content = Base64.getDecoder().decode(it.message).toString()
            eventMapper.toCompactedJSONLDMap(content) }
    }

    fun query(eventQuery: EventQuery): JsonNode? {
        val rdf = eventQueryService.executeQuery(eventQuery) ?: return null
        return eventMapper.toCompactedJSONLDMap(rdf)
    }

    private fun enrichJsonEvent(jsonEvent: String, type: EventType): EnrichedEvent {

        // only validate if a schema is attached to the type
        if (type.schemaDefinition != null) {
            val jsonValidator = JSONValidator()
            jsonValidator.validateJSON(jsonEvent, type.schemaDefinition)
        }

        val node = eventMapper.toJsonNode(jsonEvent)
        if (node.nodeType != JsonNodeType.OBJECT)
            throw UnsupportedEventTypeException("Unexpected event data, invalid JSON data!")

        node as ObjectNode
        val uuid = UUID.randomUUID()
        node.put(EVENT_UUID_FIELD, uuid.toString())
        node.put(EVENT_TYPE_FIELD, type.eventType)

        val rdf = eventMapper.toRDFTurtle(jsonNode = node, rml = type.rml)

        val enrichedEvent = EnrichedEvent(jsonEvent, type, uuid, rdf)
        if (type.minimize == true && type.minimalRml != null ) {
            enrichedEvent.strippedEventRDF = eventMapper.toRDFTurtle(jsonNode = node, rml = type.minimalRml)
        }
        return enrichedEvent
    }

    private fun validateWithShacl(enrichedEvent: EnrichedEvent) {
        val shaclValidator = ShaclValidator(eventTypeMapping.readShaclShapes())
        if (enrichedEvent.eventType.shacl != null) shaclValidator.validate(enrichedEvent.eventRDF)
    }

    private fun publishRDFEvent(enrichedEvent: EnrichedEvent, destinations: Set<String>? = null): UUID {
        val dest = destinations?.map { OrchestratorEventDestination.parse(it) }?.toSet()
        return eventDistributionService.distributeEvent(enrichedEvent = enrichedEvent, destinations = dest)
    }
}