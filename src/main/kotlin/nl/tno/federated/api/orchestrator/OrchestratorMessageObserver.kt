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
            log.info("Retrieving events for publication since last successful poll interval: {}", lastPoll)
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