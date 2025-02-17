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
 * contains: Controller to provide REST APIs to manage the distribution rules
 *
 * @property rules EventDistributionRuleService , service to initiate all distributionRule based actions
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