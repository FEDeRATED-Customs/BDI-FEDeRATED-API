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

import com.fasterxml.jackson.databind.ObjectMapper
import nl.tno.federated.api.orchestrator.config.OrchestratorConfig
import nl.tno.federated.api.util.toJsonNode
import nl.tno.federated.api.webhook.WebhookHttpClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient


@Service
class OrchestratorClientService (private val config : OrchestratorConfig, val objectMapper: ObjectMapper) {

    fun sendMessage(message: IOrchestratorMessage) {


        log.debug("Sending message : {}",message.toJsonNode(objectMapper))
        val eventMessage = objectMapper.writeValueAsString(message)
        restClient.post()
            .uri("${config.server.toURL("/api/message")}")
            .contentType(MediaType.APPLICATION_JSON)
            .headers {
               it.add("x-api-key",config.server.XApiKey)
            }
            .body(eventMessage)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, response ->
                log.warn("Sending event to orchestrator: ${config.server} failed with ${response.statusCode}")
                throw OrchestratorException("Unable to access Orchestrator API: ${response.statusCode} : ${response.body}")
            }
            .onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                log.warn("Sending event to orchestrator: ${config.server} failed with ${response.statusCode}")
                throw OrchestratorException("Error sending message to orchestrator API: ${response.statusCode} : ${response.body}")
            }
            .toBodilessEntity()

        log.debug("Sending succesfully send.")

    }

    companion object {
        private val restClient: RestClient = RestClient.create()
        private val log = LoggerFactory.getLogger(WebhookHttpClient::class.java)
    }

}