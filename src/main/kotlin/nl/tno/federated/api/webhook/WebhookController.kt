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

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/webhooks")
class WebhookController(private val webhookService: WebhookService) {

    @GetMapping
    fun getWebhooks(): List<Webhook> {
        return webhookService.getWebhooks()
    }

    @PostMapping
    fun registerWebhook(@RequestBody(required = true) registration: Webhook): Webhook {
        return webhookService.register(registration)
    }

    @DeleteMapping("/{client_id}")
    fun unregisterWebhook(@PathVariable("client_id") clientId: String): ResponseEntity<Void> {
        return if (webhookService.unregister(clientId)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }
}