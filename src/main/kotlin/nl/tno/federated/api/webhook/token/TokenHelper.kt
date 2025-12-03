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

package nl.tno.federated.api.webhook.token

import nl.tno.federated.api.webhook.Webhook
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.client.RestClient

object TokenHelper {
    fun getAccessToken(webhook: Webhook, jwtHelper: JwtHelper): AccessToken? {

        val jwt = jwtHelper.createJWT(webhook.clientId, webhook.aud!!)
        val restClient = RestClient.create()

        return restClient.post()
            .uri(webhook.tokenURL.toString())
            .contentType(APPLICATION_JSON)
            .headers {
                it.set("clientid", webhook.clientId)
                it.setBearerAuth(jwt)
            }
            .accept(APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, response ->
                throw TokenException("Unable to acquire access token")
            }
            .onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                throw TokenException("Unable to acquire access token")
            }
            .body(AccessToken::class.java)
    }

    fun renewAccessToken(webhook: Webhook, jwtHelper: JwtHelper, accessToken: AccessToken): AccessToken? {

        val restClient = RestClient.create()

        if (webhook.refreshURL != null) {
            val data = "{ \"refreshToken\"': \"${accessToken.refreshToken}\", \"grantType\": \"refreshToken\" }"
            val refreshToken = restClient.post()
                .uri(webhook.refreshURL.toString())
                .contentType(APPLICATION_JSON)
                .headers {
                    it.set("clientid", webhook.clientId)
                    it.setBearerAuth(accessToken.token)
                }
                .body(data)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError) { _, response ->
                    throw TokenException("Unable to`refresh access token: ${response.getStatusCode()}")
                }
                .onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                    throw TokenException("Unable to refresh access token: ${response.getStatusCode()}")
                }
                .body(RefreshToken::class.java)
            accessToken.token = refreshToken!!.token
            return accessToken
        } else {
            return getAccessToken(webhook,jwtHelper)
        }
    }

}