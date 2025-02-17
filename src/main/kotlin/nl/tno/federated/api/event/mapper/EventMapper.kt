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

package nl.tno.federated.api.event.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import nl.tno.federated.api.event.type.EventType
import nl.tno.federated.api.rml.RMLMapper
import nl.tno.federated.api.util.RDFUtils.convert
import nl.tno.federated.api.util.toJsonNode
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.helpers.JSONLDMode

import org.springframework.stereotype.Service

open class EventMapperException(msg: String, throwable: Throwable? = null) : Exception(msg, throwable)
class UnsupportedEventTypeException(msg: String) : EventMapperException(msg)

@Service
class EventMapper(
    private val objectMapper: ObjectMapper
) {
    private val rmlMapper = RMLMapper()

    fun toJsonNode(event: String): JsonNode {
        return event.toJsonNode(objectMapper)
    }

    fun toRDFTurtle(jsonNode: JsonNode, eventType: EventType): String {
        val json = objectMapper.writeValueAsString(jsonNode)
        return rmlMapper.createTriples(json, eventType.rml) ?: throw EventMapperException("Unable to map event to RDF, no output from mapping.")
    }

    fun toCompactedJSONLD(rdf: String): String {
        return convert(rdf, RDFFormat.TURTLE, RDFFormat.JSONLD, JSONLDMode.COMPACT)
    }

    fun toCompactedJSONLDMap(rdf: String): JsonNode {
        val bla = convert(rdf, RDFFormat.TURTLE, RDFFormat.JSONLD, JSONLDMode.COMPACT)
        return objectMapper.readTree(bla)
    }

    fun toFlattenedJSONLD(rdf: String): String {
        return convert(rdf, RDFFormat.TURTLE, RDFFormat.JSONLD, JSONLDMode.FLATTEN)
    }
}
