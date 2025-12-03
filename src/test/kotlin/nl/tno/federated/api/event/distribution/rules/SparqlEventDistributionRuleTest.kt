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

import org.eclipse.rdf4j.model.vocabulary.FOAF
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparqlEventDistributionRuleTest {

    private val rdfEventString = """
        @prefix ex: <http://example.org/> .
        @prefix foaf: <http://xmlns.com/foaf/0.1/> .
        
        ex:Stephan a ex:Developer ;
                   foaf:firstName "Stephan" ;
                   foaf:surname "Oudmaijer";
                   ex:homeAddress _:node1 .
        
        _:node1 ex:street "Tempeltuinlaan 8" ;
                ex:city "Vleuten" ;
                ex:country "The Netherlands" .
    """.trimIndent()

    private val sparql = """
        PREFIX foaf: <${FOAF.NAMESPACE}>
   
        ASK  { ?x foaf:surname  "Oudmaijer" }
    """.trimIndent()

    private val destinations = setOf("O=TNO,L=Den Haag,C=NL")

    private val rule = SparqlEventDistributionRule(sparql = sparql, destinations = destinations)


    @Test
    fun getDestinations() {
        assertEquals(destinations, rule.getDestinations())
    }

    @Test
    fun appliesTo() {
        assertTrue { rule.appliesTo(rdfEventString) }
    }
}