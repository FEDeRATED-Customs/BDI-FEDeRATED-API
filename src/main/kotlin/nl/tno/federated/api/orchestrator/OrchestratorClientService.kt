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