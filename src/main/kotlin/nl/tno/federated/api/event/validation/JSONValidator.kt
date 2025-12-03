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

import net.pwall.json.schema.JSONSchema
import org.slf4j.LoggerFactory

class JSONValidationException(msg: String) : Exception(msg)

class JSONValidator {

    private val log = LoggerFactory.getLogger(JSONValidator::class.java)

    fun validateJSON(json: String,schema: String) {
        val JSONschema = JSONSchema.parse(schema)
        val output = JSONschema.validateBasic(json)
        if (!output.valid ) {
            log.debug("JSON validation failed for event data: {}", json)
            val builder = StringBuilder()
            builder.append("The JSON event provided does not match the required definition: \n")
            output.errors?.forEach {
                builder.append("${it.error} - ${it.instanceLocation} \n")
            }
            throw JSONValidationException(builder.toString())
        }
    }
}