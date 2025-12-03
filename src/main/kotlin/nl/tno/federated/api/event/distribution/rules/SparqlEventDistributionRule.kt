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
package nl.tno.federated.api.event.distribution.rules

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.sail.memory.MemoryStore
import java.io.StringReader

class SparqlEventDistributionRule(
    private val sparql: String,
    private val destinations: Set<String>
) : EventDistributionRule {

    override fun getDestinations() = destinations

    override fun appliesTo(ttl: String): Boolean {
        val db: Repository = SailRepository(MemoryStore())
        db.init()

        try {
            db.connection.use { conn ->
                StringReader(ttl).use {
                    conn.add(it, "", RDFFormat.TURTLE)
                }
                val query = conn.prepareBooleanQuery(sparql)
                return query.evaluate()
            }
        } finally {
            db.shutDown()
        }
    }

    override fun toString(): String {
        return "SparqlEventDistributionRule(destinations='${getDestinations().joinToString(",")}'"
    }
}