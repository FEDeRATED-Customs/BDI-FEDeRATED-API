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

import nl.tno.federated.api.webhook.OrchestratorEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * The EventObserver retrieves events from the history database.
 *
 * Initial value for last poll interval is set to the startup time of the application. This means that
 * during downtime of this application, no events are sent to Webhooks, neither are events that were
 * received during the downtime.
 */
@Service
class OrchestratorMessageObserver(private val orchestratorService: OrchestratorService, private val applicationEventPublisher: ApplicationEventPublisher) {

    private val log = LoggerFactory.getLogger(OrchestratorMessageObserver::class.java)
    private var lastPoll:  Long = Instant.now().epochSecond

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    fun observe() {
        try {
            log.info("Retrieving events for publication since last successful poll interval: {}", Instant.ofEpochSecond(lastPoll))
            val result = orchestratorService.findEventsIncomingAfter(lastPoll,1, 500, MessageType.EVENT )
            log.info("{} events retrieved from incoming history for publication.", result.size)

            if(result.isNotEmpty()) {

                result.forEach {
                    log.info("Publishing event...")
                    applicationEventPublisher.publishEvent(OrchestratorEvent(it.eventType!!, it.eventRDF!!, it.eventUUID))
                }
                // Update the last poll timestamp to last recordedTime from list of events
                lastPoll = lastRecordedTimestamp(result).eventRecorded!!
            }
            else
                lastPoll = Instant.now().epochSecond
        }
        catch (e: Exception) {
            log.warn("Failed to fetch events for publication: {}", e.message)
        }
    }

    private fun lastRecordedTimestamp(list: List<OrchestratorContent>): OrchestratorContent {
        return list.maxBy { it.eventRecorded!! }
    }
}