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
package nl.tno.federated.api.event.validation

import org.junit.Test

class ShaclValidatorTest {

    @Test
    fun validate() {
        val shape = """
            @prefix ex: <http://example.org#> .
            @prefix dash: <http://datashapes.org/dash#> .
            @prefix sh: <http://www.w3.org/ns/shacl#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            @prefix foaf:  <http://xmlns.com/foaf/0.1/> .
            
            ex:PersonShape a sh:NodeShape ;
                sh:targetClass foaf:Person ;
                sh:property [
                   sh:path foaf:birthday ;
                   sh:datatype xsd:date ;
                ] .
        """.trimIndent()

        val rdf = """
            @prefix laureate: <http://data.nobelprize.org/resource/laureate/> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix foaf:  <http://xmlns.com/foaf/0.1/> .
            
            laureate:935
                a                foaf:Person ;
                foaf:birthday    "1948-10-09"^^xsd:date ;
                foaf:familyName  "Hart" ;
                foaf:givenName   "Oliver" ;
                foaf:name        "Oliver Hart" ;
                foaf:gender      "male" .     
        """.trimIndent()

        val validator = ShaclValidator(listOf(shape))
        validator.validate(rdf)
    }

    @Test(expected = ShaclValidationException::class)
    fun validateExceptionThrown() {
        val shape = """
            @prefix ex: <http://example.org#> .
            @prefix dash: <http://datashapes.org/dash#> .
            @prefix sh: <http://www.w3.org/ns/shacl#> .
            @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
            @prefix foaf:  <http://xmlns.com/foaf/0.1/> .
            
            ex:PersonShape a sh:NodeShape ;
                sh:targetClass foaf:Person ;
                sh:property [
                   sh:path foaf:birthday ;
                   sh:datatype xsd:string ;
                ] .
        """.trimIndent()

        val rdf = """
            @prefix laureate: <http://data.nobelprize.org/resource/laureate/> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix foaf:  <http://xmlns.com/foaf/0.1/> .
            
            laureate:935
                a                foaf:Person ;
                foaf:birthday    "1948-10-09"^^xsd:date ;
                foaf:familyName  "Hart" ;
                foaf:givenName   "Oliver" ;
                foaf:name        "Oliver Hart" ;
                foaf:gender      "male" .     
        """.trimIndent()

        val validator = ShaclValidator(listOf(shape))
        validator.validate(rdf)
    }

}