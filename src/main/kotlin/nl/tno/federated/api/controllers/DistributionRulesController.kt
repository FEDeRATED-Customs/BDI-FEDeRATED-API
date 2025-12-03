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

import io.swagger.v3.oas.annotations.tags.Tag
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleDTO
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleEntity
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleService
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleType
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/distribution-rules", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "DistributionRulesController", description = "Returns info regarding the distribution roles for this node.")
class DistributionRulesController(private val rules: EventDistributionRuleService) {

    @GetMapping
    fun getDistributionRoles() = rules.getDistributionRules()

    @PostMapping
    fun postDistributionRule(@RequestBody eventDistributionRule: EventDistributionRuleDTO): EventDistributionRuleDTO {
        val entity = rules.addDistributionRule(fromDTO(eventDistributionRule))
        return fromEntity(entity)
    }

    @DeleteMapping("/{id}")
    fun deleteDistributionRule(@PathVariable("id") id: Long) : ResponseEntity<Void> {
        rules.delete(id)
        return ResponseEntity.noContent().build()
    }

    private fun fromDTO(dto: EventDistributionRuleDTO): EventDistributionRuleEntity {
        return EventDistributionRuleEntity( dto.id, EventDistributionRuleType.valueOf(dto.ruleType.trim().uppercase()), dto.destinations)
    }

    private fun fromEntity(entity: EventDistributionRuleEntity): EventDistributionRuleDTO {
        return EventDistributionRuleDTO(
            id = entity.id,
            ruleType = entity.ruleType.name,
            destinations = entity.destinations
        )
    }
}