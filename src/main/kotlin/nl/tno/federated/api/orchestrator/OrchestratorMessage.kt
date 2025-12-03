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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.validation.constraints.NotNull
import nl.tno.federated.api.event.distribution.orchestrator.OrchestratorEventDestination
import java.util.*

enum class OrchestratorMessageStatus(@JsonValue val type: String) {
    CREATED("created"),
    SEND("send"),
    FAILED("failed"),
    REFUSED("refused"),
    RECEIVED("received"),
    FORWARDED("forwarded"),
    INVALID("invalid")

}
enum class DistributionType(@JsonValue val type: String) {
    STATIC ("static"),
    BROADCAST ("broadcast")
}

enum class MessageType (@JsonValue val type: String) {
    EVENT ("event"),
    FULLEVENTREQUEST ("fullevent")
}

data class OrchestratorMessage(
    @NotNull val recordedTime: Long,
    @NotNull val status: OrchestratorMessageStatus,
    val origin: String?,
    val distributionType: DistributionType?,
    val destination: String?,
    @NotNull override val messageId: UUID,
    @NotNull override val messageType: MessageType,
    @NotNull override val message: String,
    @JsonIgnore val originalJSON: String?,
    @JsonIgnore val eventType: String?
 ) : IOrchestratorMessage {

     override fun toEntity(): OrchestratorMessageEntity {
        return OrchestratorMessageEntity(
            recordedTime = recordedTime,
            status = status,
            destinations = destination,
            origin = origin,
            distributionType = distributionType,
            messageId = messageId,
            messageType = messageType,
            message = message,
            originalJSON = originalJSON,
            eventType= eventType

        )
    }
 }

data class OutgoingOrchestratorMessage (
    @NotNull val recordedTime: Long,
    @NotNull val distributionRule: DistributionType,
    @NotNull val destinations: Set<String> = emptySet<String>(),
    @NotNull override val messageId: UUID,
    @NotNull override val messageType: MessageType,
    @NotNull override val message: String,
    @JsonIgnore val originalJson: String? = null,
    @JsonIgnore val eventType: String? = null)  : IOrchestratorMessage {

    override fun toEntity(): OrchestratorMessageEntity {
        return OrchestratorMessageEntity(
            recordedTime = recordedTime,
            status = OrchestratorMessageStatus.CREATED,
            distributionType = distributionRule,
            destinations = destinations.joinToString(separator = ","),
            messageId = messageId,
            messageType = messageType,
            message = message,
            originalJSON = originalJson,
            eventType = eventType

        )
    }

    companion object {
        fun build(recordedTime: Long, destinations: Set<OrchestratorEventDestination>, type: MessageType, message: String, messageId: UUID?, originalJSON: String? = null, fullEvent: String? =null ): IOrchestratorMessage {
            val destinationString = destinations.map{it.destination}.toSet()

            val distribution = when {   destinationString.isEmpty () ->  DistributionType.BROADCAST
                                        else -> DistributionType.STATIC
                                    }
             return OutgoingOrchestratorMessage(recordedTime, distribution, destinationString , messageId?: UUID.randomUUID(), type, message, originalJSON, fullEvent)
        }
    }
}

data class IncomingOrchestratorMessage (
    @NotNull val recordedTime: Long,
    @NotNull override val messageId: UUID,
    @NotNull override val messageType: MessageType,
    @NotNull override val message: String,
    @NotNull val origin: String,
    val eventType: String?
) : IOrchestratorMessage {

    override fun toEntity(): OrchestratorMessageEntity {
        return OrchestratorMessageEntity(
            recordedTime = recordedTime,
            status = OrchestratorMessageStatus.RECEIVED,
            origin = origin,
            messageId = messageId,
            messageType = messageType,
            message = message,
            eventType = eventType

        )
    }
}

interface IOrchestratorMessage {
    val messageId: UUID
    val messageType: MessageType
    val message: String

    fun toEntity() : OrchestratorMessageEntity

}
