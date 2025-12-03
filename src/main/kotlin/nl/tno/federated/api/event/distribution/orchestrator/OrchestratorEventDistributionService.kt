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