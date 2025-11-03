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
 * contains: Controller to provide REST APIs to send and receive the events
 *
 * @property eventService EventService , service to initiate all event related actions
 * @property graphDBService GraphDBService, service to initiate all graph DB related actions
 * @property orchestratorService OrchestratirService, service to initiate all Orchestrator related actions
 */

package nl.tno.federated.api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import nl.tno.federated.api.event.EventService
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDestination
import nl.tno.federated.api.orchestrator.OrchestratorService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

const val EVENT_TYPE_HEADER = "Event-Type"
const val EVENT_DESTINATION_HEADER = "Event-Destinations"

@RestController
@RequestMapping("/api/events")
@Tag(name = "EventsController", description = "Allows for creation, distribution and retrieval of events. See the /event-types endpoint for all supported event types by this node.")
class EventsController(
    private val eventService: EventService,
    private val orchestratorService: OrchestratorService
) {

    companion object {
        private val log = LoggerFactory.getLogger(EventsController::class.java)
    }

    @Operation(summary = "Return the event data in compacted JSONLD format.")
    @GetMapping(path = ["/{id}"], produces = [APPLICATION_JSON_VALUE])
    fun getEventById(@PathVariable("id") id: String): ResponseEntity<String> {
        log.info("Get event by ID: {}", id)
        return ResponseEntity.ok(orchestratorService.findMessageById(id)?.message)
    }

    @Operation(summary = "Return the event data in compacted JSONLD format.")
    @GetMapping(path = ["/fullevent/{id}"], produces = [APPLICATION_JSON_VALUE])
    fun requestFullEventById(@RequestHeader(name = EVENT_DESTINATION_HEADER, required = false) eventDestination: String?, @PathVariable("id") id: String): ResponseEntity<String> {
        log.info("Request Full event ID: ${id}")
        val destination = eventDestination?.split(";")?.toSet()
        if (destination == null) {
            return ResponseEntity.badRequest().body("Missing required \"Event-Destinations\" header")
        }
        if (destination.size > 1) {
            return ResponseEntity.badRequest().body("Only allowed to request a full event from on destination")
        }
        val destinations = destination.map { OrchestratorEventDestination.parse(it) }.toSet()
        return ResponseEntity.ok(orchestratorService.requestFullEvent(id, destinations).toString())
    }

    @Operation(summary = "Return the event data in compacted JSONLD format.")
    @GetMapping(path = [""], produces = [APPLICATION_JSON_VALUE])
    fun getEvents(@RequestParam("page", defaultValue = "1") page: Int, @RequestParam("size", defaultValue = "25") size: Int): ResponseEntity<List<String>> {
        log.info("Get all events, page: {}, size: {}", page, size)
        if (page < 1) throw InvalidPageCriteria("Page should be greater than 0.")
        if (size < 1) throw InvalidPageCriteria("Page size should be greater than 0.")
        return ResponseEntity.ok().body(orchestratorService.findAllEvents(page-1, size))
    }

    @Operation(summary = "Create a new event and distribute to peers according to the distribution rules. The Event-Type header specifies the Event type e.g: federated.events.load-event.v1. See the /event-types endpoint for all supported event types by this node. Event-Destinations header specifies the node names to send the even to. Node names can be separated with a semi-colon (;). An example: O=Cargobase,L=Dusseldorf,C=DE")
    @PostMapping(path = [""], consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun postEvent(@RequestBody event: String, @RequestHeader(EVENT_TYPE_HEADER) eventType: String, @RequestHeader(name = EVENT_DESTINATION_HEADER, required = false) eventDestinations: String?): ResponseEntity<Void> {
        log.info("Received new event: {}", event)
        val destinations: Set<String>? = eventDestinationsToSet(eventDestinations)
        destinations?.map { OrchestratorEventDestination.parse(it) }?.toSet()
        val enrichedEvent = eventService.newJsonEvent(event, eventType, destinations)

        log.info("New event created with UUID: {}", enrichedEvent.eventUUID)
        return ResponseEntity.created(URI("/api/events/${enrichedEvent.eventUUID}")).build()
    }

    @Operation(summary = "Validate an event without distribution, returns the generated RDF if no validation errors occur. The Event-Type header specifies the type of event e.g: federated.events.load-event.v1. See the /event-types endpoint for all supported event types by this node.")
    @PostMapping(path = ["/validate"], consumes = [APPLICATION_JSON_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun validateEvent(@RequestBody event: String, @RequestHeader(EVENT_TYPE_HEADER) eventType: String): ResponseEntity<String> {
        log.info("Validate new event: {}", event)

        val rdf = eventService.validateNewJsonEvent(event, eventType)
        return ResponseEntity.ok(rdf.eventRDF)
    }

    private fun eventDestinationsToSet(eventDestinations: String?): Set<String>? {
        return eventDestinations?.split(";")?.toSet()
    }

}