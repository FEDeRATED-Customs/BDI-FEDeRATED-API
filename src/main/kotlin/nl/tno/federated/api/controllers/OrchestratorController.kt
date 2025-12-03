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

package nl.tno.federated.api.controllers

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import nl.tno.federated.api.event.fulleventrequest.FullEventRequestEvent
import nl.tno.federated.api.graphdb.GraphDBService
import nl.tno.federated.api.orchestrator.IncomingOrchestratorMessage
import nl.tno.federated.api.orchestrator.MessageType
import nl.tno.federated.api.orchestrator.OrchestratorMessageStatus
import nl.tno.federated.api.orchestrator.OrchestratorService
import nl.tno.federated.api.util.toJsonNode
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
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
    private val applicationEventPublisher: ApplicationEventPublisher
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
                                orchestratorService.updateMessageStatus(incomingMessage, OrchestratorMessageStatus.INVALID)
                            }
                        }
                    }
                    MessageType.FULLEVENTREQUEST -> {
                        // check access in the MessageLog
                        with(orchestratorService.receiveFullEventMessage(incomingMessage)) {
                            log.info("requested full event data for event with id: {}",this.eventUUID)
                            val originalMessage = orchestratorService.findMessageById(this.eventUUID.toString())
                                ?: return ResponseEntity(HttpStatus.NOT_FOUND)
                            if (originalMessage.status != OrchestratorMessageStatus.SEND || !originalMessage.destination?.contains(incomingMessage.origin)!!)
                                return ResponseEntity(HttpStatus.UNAUTHORIZED)
                            log.debug("Passed the checks (original message is send to requester), the event will be resend as a full event")
                            applicationEventPublisher.publishEvent(FullEventRequestEvent(incomingMessage.origin ,this.eventUUID))
                        }
                    }
                }
        } catch (e: Exception) {
            log.warn("Not processing message {} because: {}",incomingMessage.messageId,e.message )

        }
        log.warn("processed {}",incomingMessage.messageId )
        return ResponseEntity(HttpStatus.ACCEPTED)
    }

}