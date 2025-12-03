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

package nl.tno.federated.api.controllers

import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleService
import nl.tno.federated.api.webhook.WebhookService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class IndexController(
    private val environment: Environment,
    private val rules: EventDistributionRuleService,
    private val webhookService: WebhookService
) {

    companion object {
        private val log = LoggerFactory.getLogger(IndexController::class.java)
    }

    @RequestMapping("/")
    fun index(model: Model): String {
        model.addAttribute("version", environment.getProperty("federated.node.version"))

        model.addAttribute("graphdbSparqlUrl", environment.getProperty("federated.node.graphdb.triplestore.protocol") + "://" +
                environment.getProperty("federated.node.graphdb.triplestore.host") + ":" + environment.getProperty("federated.node.graphdb.triplestore.port") +
                "/repositories/" + environment.getProperty("federated.node.graphdb.triplestore.repository")
        )
        model.addAttribute("eventDistributionRules", getDistributionRules())
        model.addAttribute("webhooks", webhookService.getWebhooks())
        return "index"
    }

    private fun getDistributionRules() = rules.getDistributionRules()


}

private fun <T> Iterable<T>.jts(transform: ((T) -> CharSequence)? = null): String = this.joinToString(prefix = "[", postfix = "]", separator = "], [", transform = transform)