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

package nl.tno.federated.api.orchestrator

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
import java.time.Instant.now
import java.util.*

data class OrchestratorContent (
    val eventUUID: UUID,
    val eventType: String?= null,
    val eventRDF: String? = null,
    var eventRecorded: Long? = now().epochSecond
)

data class OrchestratorFullEventContent
(
    val eventUUID: UUID
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
        else OrchestratorMessage( recordedTime = result.recordedTime,
                                status = result.status,
                                origin = result.origin,
                                distributionType = result.distributionType,
                                destination = result.destinations,
                                messageId = result.messageId,
                                messageType = result.messageType,
                                message = result.message,
                                originalJSON = result.originalJSON,
                                eventType = result.eventType)
    }


    fun findAllEvents(page: Int, size: Int) : List<String> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findAll(pageable)
        return result.content.map {
            val content = objectMapper.readValue(Base64.getDecoder().decode(it.message),OrchestratorContent::class.java)
            content.eventRDF?.let { it1 -> eventMapper.toCompactedJSONLD(it1) }.toString()
        }
    }

    fun sendEventMessage(enrichedEvent: EnrichedEvent, destinations: Set<OrchestratorEventDestination>, messageId: UUID? = null): UUID {

        val message = OutgoingOrchestratorMessage.build(
            enrichedEvent.recordedTime ?: now().epochSecond,
            destinations,
            MessageType.EVENT,
            Base64.getEncoder().encodeToString(getOrchestratorContent(enrichedEvent).toByteArray()),
            messageId?: enrichedEvent.eventUUID,
            enrichedEvent.eventJson,
            enrichedEvent.eventType.eventType
            )
        addMessage(message)
        try {
            httpClientService.sendMessage(message)
        } catch (x: Exception) {
            updateMessageStatus(message, OrchestratorMessageStatus.FAILED)
            throw(x)
        }
        updateMessageStatus(message, OrchestratorMessageStatus.SEND)
        return message.messageId
    }

    fun requestFullEvent(eventID: String, destinations: Set<OrchestratorEventDestination>): UUID {
        val message = OutgoingOrchestratorMessage.build(
            now().epochSecond,
            destinations,
            MessageType.FULLEVENTREQUEST,
            Base64.getEncoder().encodeToString(getOrchestratorFullEventContent(eventID).toByteArray()),
            UUID.randomUUID()
        )
        addMessage(message)
        try {
            httpClientService.sendMessage(message)
        } catch (x: Exception) {
            updateMessageStatus(message, OrchestratorMessageStatus.FAILED)
            throw(x)
        }
        updateMessageStatus(message, OrchestratorMessageStatus.SEND)
        return message.messageId
    }

    fun receiveEventMessage(message: IncomingOrchestratorMessage): OrchestratorContent {
        val content = objectMapper.readValue(Base64.getDecoder().decode(message.message),OrchestratorContent::class.java)
        val recorded = content.eventRecorded?: now().epochSecond
        val newMessage = message.copy(recordedTime = recorded, eventType = content.eventType)
        addMessage(newMessage)
        return content
    }

    fun receiveFullEventMessage(message: IncomingOrchestratorMessage): OrchestratorFullEventContent {
        // The DO does not forward the recorded time so we have to add it ourselves
        val content = objectMapper.readValue(Base64.getDecoder().decode(message.message),OrchestratorFullEventContent::class.java)
        val newMessage = message.copy(recordedTime = now().epochSecond)
        addMessage(newMessage)
        return content
    }

    @Transactional
    fun addMessage(message: IOrchestratorMessage): OrchestratorMessageEntity
    {
        log.info("Try to insert into local message history database: orchestration message with id : ${message.messageId}")
        return orchestratorRepository.saveAndFlush(message.toEntity())
    }

    @Transactional
    fun updateMessageStatus(message: IOrchestratorMessage, status: OrchestratorMessageStatus)
    {
        log.info("update into local message history database to ${status}: orchestration message with id : ${message.messageId}")

        val orchMessage = orchestratorRepository.findByMessageId(message.messageId)
        if (orchMessage!= null) {
            orchMessage.status =status
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

    // Methods for the message viewer.
    fun findAllIncomingDOMessages(page: Int, size: Int): List<OrchestratorMessage> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findByStatusIn(listOf(OrchestratorMessageStatus.RECEIVED,OrchestratorMessageStatus.FORWARDED), pageable)
        return result.content.map{it.toOrchestratorMessage()}
    }
    fun findAllOutGoingDOMessages(page: Int, size: Int): List<OrchestratorMessage> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findByStatusIn(listOf(OrchestratorMessageStatus.SEND), pageable)
        return result.content.map{it.toOrchestratorMessage()}
    }

    fun findAllMessagesOfEventTypeBefore(offset: Long, eventType: String): List<OrchestratorMessage> {
        val result = orchestratorRepository.findByRecordedTimeLessThanAndEventType(offset, eventType )
        return result.map{it.toOrchestratorMessage()}
    }

    fun findAllFailedDOMessages(page: Int, size: Int): List<OrchestratorMessage> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result: Page<OrchestratorMessageEntity> = orchestratorRepository.findByStatusIn(listOf(OrchestratorMessageStatus.CREATED, OrchestratorMessageStatus.INVALID,OrchestratorMessageStatus.FAILED,OrchestratorMessageStatus.REFUSED), pageable)
        return result.content.map{it.toOrchestratorMessage()}
    }



    fun findIncomingAfter(offset: Long, page: Int, size: Int): List<OrchestratorContent> {
        val result = orchestratorRepository.findByRecordedTimeGreaterThanAndStatus(offset,OrchestratorMessageStatus.RECEIVED)
        val map = result.map {
            val content =
                objectMapper.readValue(Base64.getDecoder().decode(it.message), OrchestratorContent::class.java)
            OrchestratorContent(
                eventUUID = content.eventUUID,
                eventType = content.eventType,
                eventRDF = content.eventRDF,
                eventRecorded = it.recordedTime
            )
        }
        return map
    }

    fun findEventsIncomingAfter(offset: Long, page: Int, size: Int, messageType: MessageType): List<OrchestratorContent> {
        val result = orchestratorRepository.findByRecordedTimeGreaterThanAndStatusAndMessageType(offset,OrchestratorMessageStatus.RECEIVED, MessageType.EVENT)
        val map = result.map {
            val content =
                objectMapper.readValue(Base64.getDecoder().decode(it.message), OrchestratorContent::class.java)
            OrchestratorContent(
                eventUUID = content.eventUUID,
                eventType = content.eventType,
                eventRDF = content.eventRDF,
                eventRecorded = it.recordedTime
            )
        }
        return map
    }

    private fun getOrchestratorContent(event: EnrichedEvent) : String {
        val rdf = if (event.strippedEventRDF != null) event.strippedEventRDF else event.eventRDF
        return objectMapper.writeValueAsString(OrchestratorContent(event.eventUUID, event.eventType.eventType, rdf))

    }

    private fun getOrchestratorFullEventContent(eventId: String) : String {

        return objectMapper.writeValueAsString(OrchestratorFullEventContent(UUID.fromString(eventId)))

    }

    fun deleteMessage(message: OrchestratorMessageEntity) {
        orchestratorRepository.delete(message)
    }

    fun deleteMessages(messages: List<OrchestratorMessageEntity>) {
        orchestratorRepository.deleteAll(messages)
    }

    @Transactional
    fun deleteByRecordedTimeLessThanAndEventType(cutOff: Long, eventType: String) {
        orchestratorRepository.deleteByRecordedTimeLessThanAndEventType(cutOff,eventType)
    }


}