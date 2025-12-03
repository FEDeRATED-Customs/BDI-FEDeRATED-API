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

package nl.tno.federated.api.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.jsonldjava.core.JsonLdOptions
import com.github.jsonldjava.core.JsonLdProcessor
import com.github.jsonldjava.utils.JsonUtils
import java.io.StringWriter

fun Any.toJsonString(objectMapper: ObjectMapper): String {
    val sw = StringWriter()
    objectMapper.writeValue(sw, this)
    return sw.toString()
}


fun String.toJsonNode(objectMapper: ObjectMapper): JsonNode {
   return objectMapper.readTree(this)
}

fun Any.toJsonNode(objectMapper: ObjectMapper): JsonNode {
    return objectMapper.valueToTree(this)
}

fun compactJsonLD(jsonLd: String): Map<String, Any> {
    val jsonObject: Any = JsonUtils.fromString(jsonLd)
    return JsonLdProcessor.compact(jsonObject, HashMap<Any, Any>(), JsonLdOptions())
}

fun flattenJsonLD(jsonLd: String): String {
    val jsonObject: Any = JsonUtils.fromString(jsonLd)
    val result: Any = JsonLdProcessor.flatten(jsonObject, HashMap<Any, Any>(), JsonLdOptions())
    return JsonUtils.toString(result)
}