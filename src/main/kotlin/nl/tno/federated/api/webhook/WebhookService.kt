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