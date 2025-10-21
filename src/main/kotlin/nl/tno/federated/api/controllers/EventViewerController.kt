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
 * contains: Controller to provide REST APIs to send and receive the events history for the event viewer
 *
 * @property orchestratorService OrchestratorService, service to initiate all Orchestrator related actions
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