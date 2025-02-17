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

package nl.tno.federated.api.graphdb

object PrefixHandlerTTLGenerator {
    private val basePrefix = "@base <http://example.com/base/> . \n"

    private val examplePrefix = "@prefix data: <http://example.com/base#> .\n" +
        "@prefix ex: <http://example.com/base#> . \n"

    private val semanticElementsPrefixes = "@prefix owl: <http://www.w3.org/2002/07/owl#> . \n" +
        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"

    private val ttlGeneratorPrefixes = basePrefix + semanticElementsPrefixes + examplePrefix +
        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
        "@prefix time: <http://www.w3.org/2006/time#> . \n"

    private val prefixOntologyObjectsMap = mapOf(
        OntologyObjects.PhysicalInfrastructure to "@prefix pi: <https://ontology.tno.nl/logistics/federated/PhysicalInfrastructure#> .\n",
        OntologyObjects.Event to "@prefix Event: <https://ontology.tno.nl/logistics/federated/Event#> . \n",
        OntologyObjects.BusinessService to "@prefix businessService: <https://ontology.tno.nl/logistics/federated/BusinessService#> .\n",
        OntologyObjects.DigitalTwin to "@prefix dt: <https://ontology.tno.nl/logistics/federated/DigitalTwin#> .\n",
        OntologyObjects.Classifications to "@prefix classifications: <https://ontology.tno.nl/logistics/federated/Classifications#> .\n"
    )

    fun getPrefixesTTLGenerator(): String {
        return ttlGeneratorPrefixes + examplePrefix +
            prefixOntologyObjectsMap[OntologyObjects.Event] +
            prefixOntologyObjectsMap[OntologyObjects.PhysicalInfrastructure] +
            prefixOntologyObjectsMap[OntologyObjects.BusinessService] +
            prefixOntologyObjectsMap[OntologyObjects.DigitalTwin] +
            prefixOntologyObjectsMap[OntologyObjects.Classifications]
    }
}

object PrefixHandlerQueries {
    private val semanticElementsPrefixes = "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "

    private val prefixOntologyObjectsMap = mapOf(
        OntologyObjects.PhysicalInfrastructure to "PREFIX pi: <https://ontology.tno.nl/logistics/federated/PhysicalInfrastructure#> ",
        OntologyObjects.Event to "PREFIX Event: <https://ontology.tno.nl/logistics/federated/Event#> ",
        OntologyObjects.BusinessService to "PREFIX businessService: <https://ontology.tno.nl/logistics/federated/BusinessService#> ",
        OntologyObjects.DigitalTwin to "PREFIX dt: <https://ontology.tno.nl/logistics/federated/DigitalTwin#> ",
        OntologyObjects.Classifications to "PREFIX classifications: <https://ontology.tno.nl/logistics/federated/Classifications#> "
    )

    fun getPrefixesEvent(): String {
        return prefixOntologyObjectsMap[OntologyObjects.Event]!!
    }

    fun getPrefixesDigitalTwin(): String {
        return prefixOntologyObjectsMap[OntologyObjects.DigitalTwin]!!
    }

    fun getPrefixesSemanticElements(): String {
        return semanticElementsPrefixes
    }
}

enum class OntologyObjects {
    PhysicalInfrastructure,
    Event,
    BusinessService,
    DigitalTwin,
    Classifications
}