
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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import nl.tno.federated.api.orchestrator.OrchestratorService
import nl.tno.federated.api.util.toJsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/eventviewer")
@Tag(name = "EventViewerController", description = "Allows for receiving and retrieval of message history.")
class EventViewerController(
    private val orchestratorService: OrchestratorService,
) {

    companion object {
        private var objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule()).configure(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).setSerializationInclusion(JsonInclude.Include.NON_NULL)
        private val log = LoggerFactory.getLogger(EventViewerController::class.java)
    }

    init {
        objectMapper.registerModules(JavaTimeModule())
    }

    @Operation(summary = "Return the event data in compacted JSONLD format.")
    @GetMapping(path = ["/{inorOutorFailed}"], produces = [APPLICATION_JSON_VALUE])
    fun getInOrOut(@PathVariable("inorOutorFailed") inorOutorFailed: String, @RequestParam("page", defaultValue = "1") page: Int, @RequestParam("size", defaultValue = "25") size: Int): ResponseEntity<List<JsonNode>> {
        log.info("Get all messages based on in or out, page: {}, size: {}", page, size)
        if (page < 1) throw InvalidPageCriteria("Page should be greater than 0.")
        if (size < 1) throw InvalidPageCriteria("Page size should be greater than 0.")
        val messages = when (inorOutorFailed) {
            "in" -> orchestratorService.findAllIncomingDOMessages(page-1,size)
            "out" -> orchestratorService.findAllOutGoingDOMessages(page-1,size)
            "failed" -> orchestratorService.findAllFailedDOMessages(page-1,size)
            else -> orchestratorService.findAllMessages(page-1,size)
        }
        return ResponseEntity.ok(messages.map{ it.toJsonNode(objectMapper) })
    }
}