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
 * contains: Controller to provide REST APIs to send and receive message from and to the Distribution Orchestrator
 *
 * @property graphDBService GraphDBService, service to initiate all graph DB related actions
 * @property orchestratorService OrchestratirService, service to initiate all Orchestrator related actions
*/

package nl.tno.federated.api.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import nl.tno.federated.api.event.EventService
import nl.tno.federated.api.graphdb.GraphDBService
import nl.tno.federated.api.orchestrator.IncomingOrchestratorMessage
import nl.tno.federated.api.orchestrator.MessageType
import nl.tno.federated.api.orchestrator.OrchestratorService
import nl.tno.federated.api.util.toJsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/message")
@Tag(name = "OrchestratorController", description = "Allows for receiving and retrieval of events.")
class OrchestratorController(
    private val orchestratorService: OrchestratorService,
    private val graphDBService: GraphDBService,
    private val eventService: EventService
) {

    companion object {
        private var objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule()).configure(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL)
        private val log = LoggerFactory.getLogger(OrchestratorController::class.java)
    }

    init {
        objectMapper.registerModules(JavaTimeModule())
    }

    @Operation(summary = "Return the event data in compacted JSONLD format.")
    @GetMapping(path = ["/{id}"], produces = [APPLICATION_JSON_VALUE])
    fun getMessageById(@PathVariable("id") id: String): ResponseEntity<JsonNode?> {
        log.info("Get message by ID: {}", id)
        val message = orchestratorService.findMessageById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(message.toJsonNode(objectMapper))
    }

    @Operation(summary = "Return the event data in compacted JSONLD format.")
    @GetMapping(path = [""], produces = [APPLICATION_JSON_VALUE])
    fun getAll(@RequestParam("page", defaultValue = "1") page: Int, @RequestParam("size", defaultValue = "25") size: Int): ResponseEntity<List<JsonNode>> {
        log.info("Get all messages, page: {}, size: {}", page, size)
        if (page < 1) throw InvalidPageCriteria("Page should be greater than 0.")
        if (size < 1) throw InvalidPageCriteria("Page size should be greater than 0.")
        val messages = orchestratorService.findAllMessages(page-1,size)
        return ResponseEntity.ok(messages.map{ it.toJsonNode(objectMapper) })
    }

    @Operation(summary = "Receive a new event or a full event request from the orchestrator and store it in the database.")
    @PostMapping(path = [""], consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun postMessage(@RequestBody incomingMessage: IncomingOrchestratorMessage): ResponseEntity<String?> {
        log.info("Received new message: {}", incomingMessage)
        try {
                when (incomingMessage.messageType) {
                    MessageType.EVENT -> {
                        with(orchestratorService.receiveEventMessage(incomingMessage)) {
                            if (!graphDBService.insertEvent(this.eventRDF!!)) {
                                orchestratorService.updateMessageToInvalid(incomingMessage)
                            }
                        }
                    }
                    MessageType.QUERY -> {
                        // check access with dip
                        with(orchestratorService.receiveQueryMessage(incomingMessage)) {

                        }

                        // schedule query for running and send result as a new event message
                       // val queryResult = eventService.queryByEventId(this.eventUUID)
                    }
                    MessageType.RESULT -> {
                        // store result RDF in database
                    }
                }
        } catch (e: Exception) {
            log.warn("Not processing message {} because: {}",incomingMessage.messageId,e.message )
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        log.warn("processed {}",incomingMessage.messageId )
        return ResponseEntity(HttpStatus.ACCEPTED)
    }


}