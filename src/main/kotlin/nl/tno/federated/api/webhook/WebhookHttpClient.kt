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