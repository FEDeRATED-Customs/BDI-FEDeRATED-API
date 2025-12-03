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

import jakarta.persistence.*

import java.net.URL

@Entity
@Table(name = "API_WEBHOOK")
class WebhookEntity(

    @Id
    var id: Long? = null,
    @Column(name = "CLIENT_ID", columnDefinition="TEXT")
    val clientId: String,
    @Column(name = "EVENT_TYPE", columnDefinition="TEXT")
    val eventType: String,
    @Column(name = "CALLBACK_URL", columnDefinition="TEXT")
    val callbackURL: String,
    @Column(name = "APIKEY", columnDefinition="TEXT")
    val apiKey: String?,
    @Column(name = "TOKEN_URL", columnDefinition="TEXT")
    val tokenURL: String?,
    @Column(name = "REFRESH_URL", columnDefinition="TEXT")
    val refreshURL: String?,
    @Column(name = "AUD", columnDefinition="TEXT")
    val aud: String?

) {
    fun toWebhook() = Webhook(clientId, eventType, URL(callbackURL), apiKey,
        tokenURL?.let { URL(tokenURL) }, refreshURL?.let { URL(refreshURL) }, aud)

}