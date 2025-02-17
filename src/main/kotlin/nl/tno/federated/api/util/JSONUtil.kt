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