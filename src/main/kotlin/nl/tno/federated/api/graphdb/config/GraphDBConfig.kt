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

package nl.tno.federated.api.graphdb.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "federated.node.graphdb")
class GraphDBConfig (val triplestore : Triplestore) {
    class Triplestore {
        lateinit var protocol: String
        lateinit var host: String
        var port: Int = 0
        lateinit var repository: String
        var connectTimeoutMillis: Int = 0
        var socketTimeoutMillis: Int = 0
        var maxConnectionsPerRoute: Int = 0
        var maxConnectionsTotal: Int = 0
    }

    fun toConnectURL(): String {
        return triplestore.protocol + "://" + triplestore.host + ":" + triplestore.port + "/repositories/" + triplestore.repository
    }

    fun toUpdateURL(): String {
        return triplestore.protocol + "://" + triplestore.host + ":" + triplestore.port + "/repositories/" + triplestore.repository + "/statements"
    }
}