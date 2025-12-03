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
package nl.tno.federated.api.event.fulleventrequest

import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tno.federated.api.event.EnrichedEvent
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDestination
import nl.tno.federated.api.event.mapper.EventMapper
import nl.tno.federated.api.event.type.EventTypeService
import nl.tno.federated.api.orchestrator.OrchestratorMessageStatus
import nl.tno.federated.api.orchestrator.OrchestratorRepository
import nl.tno.federated.api.orchestrator.OrchestratorService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

data class FullEventRequestEvent(val requester: String, val eventUUID: UUID)

@Service
class FullEventRequestService (private val orchestratorRepository: OrchestratorRepository,
                               private val eventTypeService : EventTypeService,
                               private val eventMapper: EventMapper,
                               private val orchestratorService: OrchestratorService,) {

    private val log = LoggerFactory.getLogger(FullEventRequestService::class.java)

    /**
     * This method is being invoked whenever new GenericEvent's are published by the ApplicationEventPublisher
     */
    @EventListener
    fun handleEvent(request: FullEventRequestEvent) {
        // find event in DOlog in database
        val sendEvent = orchestratorRepository.findByMessageId(request.eventUUID)
        if (sendEvent != null && sendEvent.status == OrchestratorMessageStatus.SEND && sendEvent.destinations!!.contains(request.requester)) {
            // Does eventType still exist ?
            val eventType = sendEvent.eventType?.let{ eventTypeService.getEventType(sendEvent.eventType)}
            if (eventType != null) {
                // translate original JSON into RDF
                val node = eventMapper.toJsonNode(sendEvent.originalJSON!!)
                node as ObjectNode
                val fullRDF = eventMapper.toRDFTurtle(jsonNode = node, rml = eventType.rml)
                //Use requester as destination for the full event
                val destinations = request.requester.split(";").toSet()
                val dest = destinations.map { OrchestratorEventDestination.parse(it) }.toSet()

                val enrichedEvent = EnrichedEvent(sendEvent.originalJSON, eventType ,UUID.randomUUID(), fullRDF)
                orchestratorService.sendEventMessage(enrichedEvent,dest, enrichedEvent.eventUUID)
            }

        }
    }
}