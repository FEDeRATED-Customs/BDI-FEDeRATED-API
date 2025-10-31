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
package nl.tno.federated.api.event.fulleventrequest


import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tno.federated.api.event.EVENT_TYPE_FIELD
import nl.tno.federated.api.event.EVENT_UUID_FIELD
import nl.tno.federated.api.event.EnrichedEvent
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDestination
import nl.tno.federated.api.event.mapper.EventMapper
import nl.tno.federated.api.event.mapper.UnsupportedEventTypeException
import nl.tno.federated.api.event.type.EventTypeService
import nl.tno.federated.api.orchestrator.OrchestratorMessageStatus
import nl.tno.federated.api.orchestrator.OrchestratorRepository
import nl.tno.federated.api.orchestrator.OrchestratorService
import org.slf4j.LoggerFactory
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

                val enrichedEvent = EnrichedEvent(sendEvent.originalJSON, eventType ,sendEvent.messageId, fullRDF)
                orchestratorService.sendMessage(enrichedEvent,dest, enrichedEvent.eventUUID)
            }

        }
    }
}