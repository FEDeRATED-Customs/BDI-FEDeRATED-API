/*
 *  Copyright (c) 2025 Netherlands Organization for Applied Scientific Research TNO
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
 */

package nl.tno.federated.api.event

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDestination
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDistributionService
import nl.tno.federated.api.event.mapper.EventMapper
import nl.tno.federated.api.event.mapper.UnsupportedEventTypeException
import nl.tno.federated.api.event.query.EventQuery
import nl.tno.federated.api.event.query.EventQueryService
import nl.tno.federated.api.event.query.graphdb.GraphDBEventQueryService
import nl.tno.federated.api.event.type.EventType
import nl.tno.federated.api.event.type.EventTypeMapping
import nl.tno.federated.api.event.type.EventTypeMappingException
import nl.tno.federated.api.event.validation.JSONValidator
import nl.tno.federated.api.event.validation.ShaclValidator
import nl.tno.federated.api.graphdb.GraphDBService
import nl.tno.federated.api.orchestrator.OrchestratorContent
import nl.tno.federated.api.orchestrator.OrchestratorRepository
import nl.tno.federated.api.orchestrator.OrchestratorService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.Instant.now
import java.util.*
import kotlin.collections.ArrayList


const val EVENT_UUID_FIELD = "UUID"
const val EVENT_RECORDEDTIMESTAMP_FIELD = "recordedTime"
const val EVENT_TYPE_FIELD = "eventType"

@Service
class EventService(
    private val eventMapper: EventMapper,
    private val eventQueryService: GraphDBEventQueryService,
    private val eventDistributionService: OrchestratorEventDistributionService,
    private val eventTypeMapping: EventTypeMapping,
    private val graphDBService: GraphDBService,
    private val orchestratorService: OrchestratorService,
    private val objectMapper: ObjectMapper,
    private val graphDBEventQueryService: GraphDBEventQueryService

    ) {

    private val log = LoggerFactory.getLogger(EventService::class.java)

    /**
     * Convert the given event to RDF.
     *
     * @throws UnsupportedEventTypeException is an unsupported Event type is encountered.
     */
    fun newJsonEvent(event: String, eventType: String, eventDestinations: Set<String>? = null): EnrichedEvent {
        val type = eventTypeMapping.getEventType(eventType) ?: throw EventTypeMappingException("EventType not found: $eventType")
        val enrichedEvent = enrichJsonEvent(event, type)
        validateWithShacl(enrichedEvent)
        graphDBService.insertEvent(enrichedEvent.eventRDF)
        publishRDFEvent(enrichedEvent, eventDestinations)
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
        node.put(EVENT_RECORDEDTIMESTAMP_FIELD, Instant.now().epochSecond)

        val rdf = eventMapper.toRDFTurtle(jsonNode = node, rml = type.rml)

        val enrichedEvent = EnrichedEvent(jsonEvent, type, uuid, replaceNamespaces(rdf, uuid.toString(), type.eventType))
        if (type.minimize == true && type.minimalRml != null ) {
            val strippedRdf = eventMapper.toRDFTurtle(jsonNode = node, rml = type.minimalRml)

            enrichedEvent.strippedEventRDF = replaceNamespaces(strippedRdf, uuid.toString(), type.eventType)
        }
        enrichedEvent.eventJson = node.toPrettyString()
        return enrichedEvent
    }

    private fun replaceNamespaces(rdf:String, eventID: String, eventType: String ): String {
        val regex = Regex("_:[0-9]*")
        val matches = regex.findAll(rdf)
        val nameSpaces = matches.map { it.value }.toSet()
        var enrichedRFD: String = rdf
        nameSpaces.forEach {
            enrichedRFD = enrichedRFD.replace(it, "<http://${eventType}/${eventID}/${it.substring(2)}>")
        }
        return enrichedRFD

    }
    private fun validateWithShacl(enrichedEvent: EnrichedEvent) {
        val shaclValidator = ShaclValidator(eventTypeMapping.readShaclShapes())
        if (enrichedEvent.eventType.shacl != null) shaclValidator.validate(enrichedEvent.eventRDF)
    }

    private fun publishRDFEvent(enrichedEvent: EnrichedEvent, destinations: Set<String>? = null): UUID {
        val dest = destinations?.map { OrchestratorEventDestination.parse(it) }?.toSet()
        return eventDistributionService.distributeEvent(enrichedEvent = enrichedEvent, destinations = dest)
    }


    @Scheduled(fixedDelay = 1440_000, initialDelay = 0)
    fun cleanUp() {
        log.info("Cleanup process waking up ....")
        val eventTypes = eventTypeMapping.getEventTypes()
        eventTypes.forEach { type ->
            val events = ArrayList<String>()
            if (type.eventLifeTime != null && type.eventLifeTime > 0) {
                val cutOff = now().epochSecond - type.eventLifeTime * 24 * 60 * 60
                val messages = orchestratorService.findAllMessagesOfEventTypeBefore(cutOff, type.eventType)
                messages.forEach { message ->
                    val content =
                        objectMapper.readValue(
                            Base64.getDecoder().decode(message.message),
                            OrchestratorContent::class.java
                        )
                    events.add("${content.eventType}/${content.eventUUID}/")
                }
                log.info("${type.eventType} : ${messages.size} events will be cleaned")
                if (events.size > 0) {
                    // now we have a list of event UUID's (both send and received) that need to be cleaned based on retention times in the eventtypes
                    log.debug("removed all DOMessages related to the following events : ${events.joinToString(",")}")
                    for (event in events) {
                        val query = """PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

DELETE {
    ?x ?y ?z .
}
WHERE {
    {
        SELECT ?s1 ?p1 ?o1
        WHERE {
            {
                SELECT DISTINCT (str(?s) as ?s_str)
                WHERE {
                    ?s a <https://ontology.tno.nl/logistics/federated/Event#Event> .
                    ?s ?p ?o .
                    FILTER(isIRI(?o))
                    FILTER regex(STR(?s),"${event}")
                    FILTER(?p != rdf:type)
                }
            }
            ?s1 ?p1 ?o1 .
            FILTER(SUBSTR(STR(?s1),3,STRLEN(STR(?s1))-3)=SUBSTR(?s_str,3,STRLEN(?s_str)-3))
            FILTER regex(STR(?s1),"${event}")
            FILTER(?p1!=rdf:type)
        }
    }
    
    ?x ?y ?z .
    FILTER(?x = ?s1 || ?z = ?o1)
    FILTER regex(STR(?x),"${event}")

}

"""
                        graphDBEventQueryService.deleteQuery(EventQuery(query))
                        log.info("removed ${event} from the GraphDB")
                    }
                } else {
                    log.info("Nothing to clean for eventType ${type.eventType}.")
                }
                orchestratorService.deleteByRecordedTimeLessThanAndEventType(cutOff, type.eventType)
            }
        }
        log.info("All done, going back to sleep .... zzzzzzzz.")

    }

}