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
import nl.tno.federated.api.webhook.token.JwtHelper
import nl.tno.federated.api.webhook.token.JwtHelper.environment
import nl.tno.federated.api.webhook.token.RefreshToken
import nl.tno.federated.api.webhook.token.TokenException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.net.URL
import kotlin.test.assertNotNull

class NTPWebhookTest {

    private val eventType = "federated.events.minimal.v1"
    private val tokenURL = URL("https://api-test.ntp.gov.sg//oauth/v1/token")
    private val refreshTokenURL = URL("https://api-test.ntp.gov.sg//oauth/v1/refreshToken")
    private val callBackURL = URL("https://call.back/to/me")
    private val clientId = "L7UQQHHOMK65YW4NEM3T"
    private val webhook = Webhook(clientId,eventType, callBackURL, null,tokenURL,refreshTokenURL,"NTP")


    @Test
    fun getPrivateKeyFromProperties() {

        assertDoesNotThrow( {
            JwtHelper.readRSAPrivateKeyFromProperties("federated.node.api.webhook.privatekey")
        } )
    }

    @Test
    fun createJWTFromProperties() {
        assertDoesNotThrow( {
            val privateKey = environment.getProperty("federated.node.api.webhook.privatekey")
                ?.let { JwtHelper.readRSAPrivateKey(it) }
            JwtHelper.createJWT(webhook.clientId,"NTP",privateKey) })
    }


    @Test
    fun testAcquireAccessToken() {
        val privateKey = JwtHelper.readRSAPrivateKeyFromProperties("federated.node.api.webhook.privatekey")
        val jwt = JwtHelper.createJWT(webhook.clientId, webhook.aud!!,privateKey)

        val restClient = RestClient.create()
        val token = restClient.post()
            .uri(webhook.tokenURL.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .headers {
                it.set("clientid", webhook.clientId)
                it.setBearerAuth(jwt)
            }
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, response ->

            }
            .onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                //WebhookHttpClient.log.warn("Requesting token to tokenURL: ${webhook.tokenURL.toString()} failed with ${response.statusCode}")
            }
            .body(AccessToken::class.java)
        assertNotNull(token)
    }

    @Test
    fun testRefreshAccessToken() {
        val privateKey = JwtHelper.readRSAPrivateKeyFromProperties("federated.node.api.webhook.privatekey")
        val jwt = JwtHelper.createJWT(webhook.clientId, webhook.aud!!,privateKey)

        val restClient = RestClient.create()
        val token = restClient.post()
            .uri(webhook.tokenURL.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .headers {
                it.set("clientid", webhook.clientId)
                it.setBearerAuth(jwt)
            }
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, response ->
                //WebhookHttpClient.log.warn("Requesting token to tokenURL: ${webhook.tokenURL.toString()} failed with ${response.statusCode}")
            }
            .onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                //WebhookHttpClient.log.warn("Requesting token to tokenURL: ${webhook.tokenURL.toString()} failed with ${response.statusCode}")
            }
            .body(AccessToken::class.java)

        val data = "{ \"refreshToken\"': \"${token!!.refreshToken}\", \"grantType\": \"refreshToken\" }"
        val refreshToken = restClient.post()
            .uri(webhook.refreshURL.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .headers {
                it.set("clientid", webhook.clientId)
                it.setBearerAuth(token.token)
            }
            .body(data)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, response ->

                throw TokenException("Unable to`refresh access token: ${response.getStatusCode()}")
            }
            .onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                throw TokenException("Unable to refresh access token: ${response.getStatusCode()}")
            }
            .body(RefreshToken::class.java)
        assertNotNull(refreshToken)
    }

    @Test
    fun registerWebhookWithoutToken () {

    }

    @Test
    fun registerWebHookWithToken() {

    }

    @Test
    fun triggerWebhooksWithoutToken() {

    }

    @Test
    fun triggerWebhooksWithToken() {

    }


}