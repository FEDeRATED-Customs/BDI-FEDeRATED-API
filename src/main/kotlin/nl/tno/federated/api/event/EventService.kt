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


    @Scheduled(fixedDelay = 1440_000, initialDelay = 15_000)
    fun cleanUp() {
        log.info("Cleanup process waking up ....")
        val eventTypes = eventTypeMapping.getEventTypes()
        var events = ArrayList<UUID>()
        eventTypes.forEach { type ->
            if (type.eventLifeTime != null && type.eventLifeTime > 0) {
                val cufOff = now().epochSecond - type.eventLifeTime * 24 * 60
                val messages = orchestratorService.findAllMessagesOfEventTypeBefore(cufOff, type.eventType)
                messages.forEach { message ->
                    val content =
                        objectMapper.readValue(
                            Base64.getDecoder().decode(message.message),
                            OrchestratorContent::class.java
                        )
                    events.add(content.eventUUID)
                }
                log.info("${type.eventType} : ${messages.size} events will be cleaned")
                orchestratorService.deleteMessages(messages.map { it.toEntity() })
            }
        }
        if (events.size > 0) {
            log.info("removed all DOMessages related to the following events : ${events.joinToString(",")}")

            // now we have a list of event UUID's (both send and received) that need to be cleaned based on retention times in the eventtypes
            /* val eventIdsString = events.joinToString(",")
            val query = " @@eventIds@@".replace("@@eventIds@@", eventIdsString)
            val result = graphDBEventQueryService.executeQuery(EventQuery(query))
            log.info("cleaning process result: ${result}")*/
        } else {
            log.info("Nothing to clean ... going back to sleep ")
        }
    }

}