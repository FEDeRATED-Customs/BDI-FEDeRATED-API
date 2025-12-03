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

import nl.tno.federated.api.webhook.token.AccessToken
import nl.tno.federated.api.webhook.token.AcquireJwtException
import nl.tno.federated.api.webhook.token.JwtHelper
import nl.tno.federated.api.webhook.token.TokenHelper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI

@Service
class WebhookHttpClient (jwtHelper: JwtHelper) {

    var tokenRepo = mutableMapOf<String, AccessToken>()
    val jwtHelper = JwtHelper
    val tokenHelper = TokenHelper
    var apiKey = "";

    fun send(event: OrchestratorEvent, webhook: Webhook) {
        log.info("Sending event: {} with UUID: {} to: {}", event.eventType, event.eventUUID, webhook.callbackURL)
        try {

            if (webhook.tokenURL != null) {
                try {
                    val token: AccessToken? = tokenRepo.get(webhook.tokenURL.toString())?: tokenHelper.getAccessToken(webhook, jwtHelper)
                    if (JwtHelper.isTokenExpired(token!!.token)) {
                        tokenHelper.renewAccessToken(webhook, jwtHelper,token)
                    }
                } catch (e: Exception) {
                    log.warn("Unable to notify Webhook: {}, for event with UUID: {},Unable to acquire accessToken", webhook, event.eventUUID)
                    throw AcquireJwtException(e.message)
                }
            }

            restClient.post()
                .uri(webhook.callbackURL.toString())
                .contentType(APPLICATION_JSON)
                .headers {
                    it.location = URI("/api/events/${event.eventUUID}")
                    tokenRepo.get(webhook.tokenURL.toString())?.let { token-> it.setBearerAuth(token.token) }
                    webhook.apiKey?.let { apiKey-> it.set("X-API-KEY",apiKey)}
                }
                .body(event)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { _, response ->
                    log.warn("Sending event to callback: ${webhook.callbackURL} failed with ${response.statusCode}")
                }
                .onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                    log.warn("Sending event to callback: ${webhook.callbackURL} failed with ${response.statusCode}")
                }
                .toBodilessEntity()

        } catch (e: Exception) {
            log.warn("Unable to notify Webhook: {}, for event with UUID: {}, error message: {}", webhook, event.eventUUID, e.message)
        }
    }

    companion object {
        private val restClient: RestClient = RestClient.create()
        private val log = LoggerFactory.getLogger(WebhookHttpClient::class.java)
    }
}