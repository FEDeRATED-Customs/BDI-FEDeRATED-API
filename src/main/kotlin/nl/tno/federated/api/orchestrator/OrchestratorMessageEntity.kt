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

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "API_ORCHESTRATOR_MESSAGE")
data class OrchestratorMessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "RECORDED_TIME")
    val recordedTime: Long,
    @Column(name = "STATUS")
    var status: OrchestratorMessageStatus,
    @Column(name = "ORIGIN",nullable = true)
    val origin: String? =  null,
    @Column(name = "DISTRIBUTION_TYPE",nullable = true)
    val distributionType: DistributionType? = null,
    @Column(name = "DESTINATIONS",nullable = true, columnDefinition="TEXT")
    val destinations: String? = null,
    @Column(name = "MESSAGE_ID", columnDefinition="UUID")
    val messageId: UUID,
    @Column(name = "MESSAGE_TYPE", columnDefinition="TEXT")
    val messageType: MessageType,
    @Column(name = "MESSAGE", columnDefinition="TEXT")
    val message: String,
    @Column(name = "ORIGINALJSON", columnDefinition="TEXT")
    val originalJSON: String? = null,
    @Column(name = "EVENTTYPE", columnDefinition="TEXT")
    val eventType: String? = null


    ) {
    fun toOrchestratorMessage(): OrchestratorMessage {
        return OrchestratorMessage(
            recordedTime = recordedTime,
            status = status,
            origin = origin,
            distributionType = distributionType,
            destination = destinations,
            messageId = messageId,
            messageType = messageType,
            message = message,
            originalJSON = originalJSON,
            eventType = eventType)
    }
}