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

package nl.tno.federated.api.graphdb

import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository
import java.io.StringWriter
import nl.tno.federated.api.event.query.EventQuery
import nl.tno.federated.api.graphdb.config.GraphDBConfig
import org.springframework.stereotype.Service

@Service
class GraphDBSPARQLClient(graphDBConfig: GraphDBConfig) {

    private val repository = SPARQLRepository(graphDBConfig.toConnectURL(), graphDBConfig.toUpdateURL())

    fun executeSPARQL(eventQuery: EventQuery): String {
        return repository.connection.use { it ->
            val query: TupleQuery = it.prepareTupleQuery(eventQuery.query)
            val sw = StringWriter()
            sw.use {
                query.evaluate(SPARQLResultsJSONWriter(it))
            }
            sw.toString()
        }
    }

    fun deleteSPARQL(eventQuery: EventQuery) {
         repository.connection.prepareUpdate(eventQuery.query).execute()
    }

}