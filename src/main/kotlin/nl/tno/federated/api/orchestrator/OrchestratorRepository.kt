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

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*


@Repository
interface OrchestratorRepository : JpaRepository<OrchestratorMessageEntity, Long>,
                                   PagingAndSortingRepository<OrchestratorMessageEntity, Long> {

    override fun findAll(pageable: Pageable) : Page<OrchestratorMessageEntity>

    override fun findAll(): List<OrchestratorMessageEntity>
    fun findByMessageId(messageId: UUID): OrchestratorMessageEntity?

    fun findByRecordedTimeGreaterThanAndStatus(recordedTime: Long, status: OrchestratorMessageStatus): List<OrchestratorMessageEntity>
    fun findByRecordedTimeGreaterThanAndStatusAndMessageType(recordedTime: Long, status: OrchestratorMessageStatus, messageType: MessageType): List<OrchestratorMessageEntity>
    fun findByRecordedTimeLessThanAndEventType(recordedTime: Long, eventType: String): List<OrchestratorMessageEntity>

    fun findByStatusIn(status: List<OrchestratorMessageStatus>,pageable: Pageable): Page<OrchestratorMessageEntity>

    fun deleteByRecordedTimeLessThanAndEventType(recordedTime: Long, eventType: String)
}


