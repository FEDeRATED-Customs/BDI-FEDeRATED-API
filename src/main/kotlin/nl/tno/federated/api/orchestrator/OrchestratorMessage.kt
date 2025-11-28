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
