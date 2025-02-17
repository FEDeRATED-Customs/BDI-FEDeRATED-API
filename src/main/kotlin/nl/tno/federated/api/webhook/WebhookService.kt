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


package nl.tno.federated.api.webhook

import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*

data class OrchestratorEvent(val eventType: String, @JsonIgnore val eventRdf: String, val eventUUID: UUID)

@Service
class WebhookService(
    private val webhookHttpClient: WebhookHttpClient,
    private val webhookRepository: WebhookRepository

) {

    /**
     * This method is being invoked whenever new GenericEvent's are published by the ApplicationEventPublisher
     */
    @EventListener
    fun handleEvent(event: OrchestratorEvent) {
        log.info("Event of type: {} with UUID: {} received for publication...", event.eventType, event.eventUUID)
        val filter = getWebhooks().filter { it.eventType == event.eventType }
        log.info("{} webhooks registered for eventType: {}", filter.size, event.eventType)
        filter.forEach { webhookHttpClient.send(event, it) }
    }

    fun getWebhooks(): List<Webhook> {
        return webhookRepository.findAll().map { it.toWebhook() }
    }

    fun register(w: Webhook): Webhook {
        val save = webhookRepository.saveAndFlush(WebhookEntity(clientId = w.clientId, eventType = w.eventType, callbackURL = w.callbackURL.toString(), apiKey=w.apiKey, tokenURL = w.tokenURL?.toString(),refreshURL = w.tokenURL?.toString() , aud= w.aud,  id = null))
        log.info("New Webhook saved with id: {}", save.id)
        return save.toWebhook()
    }

    fun unregister(clientId: String): Boolean {
        val webhooks = webhookRepository.findByClientId(clientId)
        if(webhooks.isNotEmpty()) {
            webhooks.forEach {
                webhookRepository.delete(it)
            }
            return true
        }
        return false
    }

    companion object {
        private val log = LoggerFactory.getLogger(WebhookService::class.java)
    }
}