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

package nl.tno.federated.api.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.net.URL

@ConfigurationProperties(prefix = "federated.node.orchestrator")
class OrchestratorConfig (val server : Server) {
    class Server {
        lateinit var host: String
        lateinit var XApiKey: String

        fun toURL(path: String? = "") : URL {
            return URL("${host}/${path}" )
        }

        fun toURI(path: String? = "") : URI {
            return URI("${host}/${path}")
        }
    }
}