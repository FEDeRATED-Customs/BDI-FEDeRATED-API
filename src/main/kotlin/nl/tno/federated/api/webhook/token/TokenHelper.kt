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