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

package nl.tno.federated.api.event.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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

    fun toRDFTurtle(jsonNode: JsonNode, rml: String): String {
        val json = objectMapper.writeValueAsString(jsonNode)
        return rmlMapper.createTriples(json, rml) ?: throw EventMapperException("Unable to map event to RDF, no output from mapping.")
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
