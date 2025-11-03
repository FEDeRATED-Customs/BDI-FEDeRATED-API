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
 * contains: Service to handle the distribution of an event and send the event to the Distribution Orchestrator
 *
 * @property orchestratorService OrchestratirService, service to initiate all Orchestrator related actions
 * @property rules EventDistributionRuleService , service to initiate all distributionRule related actions
 */

package nl.tno.federated.api.event.distribution.orchestrator

import nl.tno.federated.api.event.EnrichedEvent
import nl.tno.federated.api.event.distribution.EventDistributionService
import nl.tno.federated.api.event.distribution.rules.BroadcastEventDistributionRule
import nl.tno.federated.api.event.distribution.rules.EventDistributionRule
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleEntity
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleService
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleType
import nl.tno.federated.api.event.distribution.rules.StaticDestinationEventDistributionRule
import nl.tno.federated.api.orchestrator.OrchestratorService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class OrchestratorEventDistributionService(
    private val orchestratorService: OrchestratorService,
    private val rules: EventDistributionRuleService
) : EventDistributionService<OrchestratorEventDestination> {

    override fun distributeEvent(enrichedEvent: EnrichedEvent, destinations: Set<OrchestratorEventDestination>?): UUID {
        val destinationSet = destinations ?: runEventDistributionRules(enrichedEvent.eventRDF)
        log.info("Sending eventType: ${enrichedEvent.eventType.eventType} with eventUUID: ${enrichedEvent.eventUUID} to destination(s): ${destinationSet.map { it.destination }}")
        return orchestratorService.sendEventMessage(enrichedEvent, destinationSet)

    }

    private fun runEventDistributionRules(eventRdf: String): Set<OrchestratorEventDestination> {
        val rule = getDistributionRules().first { it.appliesTo(eventRdf) }
        log.info("Using first matching rule for event that was found: {}", rule)
        return rule
            .getDestinations()
            .map { OrchestratorEventDestination.parse(it) }
            .toSet()
    }

    private fun EventDistributionRuleEntity.toEventDistributionRule(): EventDistributionRule {
        val parsed = this.destinations.split(";")

        return when (this.ruleType) {
            EventDistributionRuleType.STATIC -> StaticDestinationEventDistributionRule(parsed.toSet())
            EventDistributionRuleType.SPARQL -> TODO("Not implemented yet")
            EventDistributionRuleType.BROADCAST -> BroadcastEventDistributionRule()
        }
    }

    fun getDistributionRules(): List<EventDistributionRule> {
        val rules = rules.getDistributionRules()

        return if (rules.none()) {
            log.info("No rules configured, returning 'broadcast' event distribution mode as default option.")
            listOf(BroadcastEventDistributionRule())
        } else {
            rules.map { it.toEventDistributionRule() }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrchestratorEventDistributionService::class.java)
    }
}