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

package nl.tno.federated.api.orchestrator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import nl.tno.federated.api.event.EnrichedEvent
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDestination
import nl.tno.federated.api.event.mapper.EventMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

data class OrchestratorContent (
    val eventUUID: UUID,
    val eventType: String,
    val eventRDF: String,
    var eventRecorded: Instant? = Instant.now()
)

@Service
class OrchestratorService(
    private val orchestratorRepository: OrchestratorRepository,
    private val objectMapper: ObjectMapper,
    private val eventMapper: EventMapper,
    private val httpClientService: OrchestratorClientService,
) {

    private val log = LoggerFactory.getLogger(OrchestratorService::class.java)

    fun findMessageById(messageId: String): OrchestratorMessage? {
        val result = orchestratorRepository.findByMessageId(UUID.fromString(messageId))

        return if(result == null)  null
        else OrchestratorMessage( date = result.date,
                                status = result.status,
                                origin = result.origin,
                                distributionType = result.distributionType,
                                destination = result.destinations,
                                messageId = result.messageId,
                                messageType = result.messageType,
                                message = result.message)
    }

    fun findEventById(id: String) : String? {
        val message = orchestratorRepository.findByMessageId(UUID.fromString(id))
        return if (message != null)  {
            val content = objectMapper.readValue(Base64.getDecoder().decode(message.message),OrchestratorContent::class.java)
            eventMapper.toCompactedJSONLD(content.eventRDF)
        } else {
            return null
        }
    }

    fun findAllEvents() : List<JsonNode> {
        val result = orchestratorRepository.findAll()
        return result.map {
            val content = objectMapper.readValue(Base64.getDecoder().decode(it.message),OrchestratorContent::class.java)
            eventMapper.toJsonNode(content.eventRDF)
        }
    }

    fun findAllEvents(page: Int, size: Int) : List<String> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findAll(pageable)
        return result.content.map {
            val content = objectMapper.readValue(Base64.getDecoder().decode(it.message),OrchestratorContent::class.java)
            eventMapper.toCompactedJSONLD(content.eventRDF)
        }
    }

    private fun getOrchestratorContent(event: EnrichedEvent) : String {
      return objectMapper.writeValueAsString(OrchestratorContent(event.eventUUID, event.eventType.eventType, event.eventRDF))

    }
    fun sendMessage(enrichedEvent: EnrichedEvent, destinations: Set<OrchestratorEventDestination>): UUID {
        val message = OutgoingOrchestratorMessage.build(
            destinations,
            MessageType.EVENT,
            Base64.getEncoder().encodeToString(getOrchestratorContent(enrichedEvent).toByteArray()),
            enrichedEvent.eventUUID)
        httpClientService.sendMessage(message)
        addMessage(message)
        return message.messageId
    }

    fun receiveMessage(message: IncomingOrchestratorMessage): OrchestratorContent {
        val inserted = addMessage(message)
        return objectMapper.readValue(Base64.getDecoder().decode(inserted.message),OrchestratorContent::class.java)
    }

    @Transactional
    fun addMessage(message: IOrchestratorMessage): OrchestratorMessageEntity
    {
        log.info("Try to insert into local message history database: orchestration message with id : ${message.messageId}")
        return orchestratorRepository.saveAndFlush(message.toEntity())
    }

    @Transactional
    fun updateMessage(message: IOrchestratorMessage): OrchestratorMessageEntity
    {
        log.info("update into local message history database: orchestration message with id : ${message.messageId}")
        return orchestratorRepository.saveAndFlush(message.toEntity())
    }


    @Transactional
    fun updateMessageToInvalid(message: IOrchestratorMessage)
    {
        log.info("update into local message history database to invalid: orchestration message with id : ${message.messageId}")

        val orchMessage = orchestratorRepository.findByMessageId(message.messageId)
        if (orchMessage!= null) {
            orchMessage.status = OrchestratorMessageStatus.INVALID
            orchestratorRepository.saveAndFlush(orchMessage)
        }
    }

    fun findAllMessages(): List<OrchestratorMessage> {
        val result = orchestratorRepository.findAll()
        return result.map { it.toOrchestratorMessage()}

    }

    fun findAllMessages(page: Int, size: Int): List<OrchestratorMessage> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findAll(pageable)
        return result.content.map{it.toOrchestratorMessage()}
    }

    fun findIncomingMessages(page: Int, size: Int): List<OrchestratorMessage> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findByStatusIn(listOf(OrchestratorMessageStatus.RECEIVED,OrchestratorMessageStatus.FORWARDED), pageable)
        return result.content.map{it.toOrchestratorMessage()}
    }
    fun findAllOutGoingMessages(page: Int, size: Int): List<OrchestratorMessage> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findByStatusIn(listOf(OrchestratorMessageStatus.SEND), pageable)
        return result.content.map{it.toOrchestratorMessage()}
    }
    fun findAllFailedMessages(page: Int, size: Int): List<OrchestratorMessage> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findByStatusIn(listOf(OrchestratorMessageStatus.INVALID,OrchestratorMessageStatus.FAILED,OrchestratorMessageStatus.REFUSED), pageable)
        return result.content.map{it.toOrchestratorMessage()}
    }



    fun findIncomingAfter(offset: Instant, page: Int, size: Int): List<OrchestratorContent> {
        val result = orchestratorRepository.findByDateAfterAndStatus(offset, PageRequest.of(page, size),OrchestratorMessageStatus.RECEIVED)
        val map = result.content.map {
            val content =
                objectMapper.readValue(Base64.getDecoder().decode(it.message), OrchestratorContent::class.java)
            OrchestratorContent(
                eventUUID = content.eventUUID,
                eventType = content.eventType,
                eventRDF = content.eventRDF,
                eventRecorded = it.date
            )
        }
        return map
    }
}