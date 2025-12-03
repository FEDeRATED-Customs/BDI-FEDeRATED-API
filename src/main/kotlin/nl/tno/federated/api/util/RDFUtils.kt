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

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.WriterConfig
import org.eclipse.rdf4j.rio.helpers.JSONLDMode
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.InputStream
import java.io.StringWriter


@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class InvalidRDFException(message: String?, e: Exception) : Exception(message, e)

object RDFUtils {

    val log = LoggerFactory.getLogger(RDFUtils::class.java)

    fun isValidRDF(input: String, format: RDFFormat): Boolean = isValidRDF(input.byteInputStream(), format)

    fun isValidRDF(input: InputStream, format: RDFFormat): Boolean {
        return try {
            input.use {
                val model = Rio.parse(input, format)
                // assert a certain event here?
                log.debug("Valid RDF data: {}", model.toString())
            }
            true
        } catch (e: Exception) {
            throw InvalidRDFException(e.message, e)
        }
    }

    fun parse(input: String, format: RDFFormat): Model = parse(input.byteInputStream(), format)

    fun parse(input: InputStream, format: RDFFormat): Model {
        return input.use {
            val model = Rio.parse(input, format)
            // assert a certain event here?
            log.debug("Valid RDF data: {}", model.toString())

            model
        }
    }

    fun convert(input: String, inputFormat: RDFFormat, outputFormat: RDFFormat, mode: JSONLDMode): String {
        input.byteInputStream().use {
            val model = Rio.parse(it, inputFormat)
            val sw = StringWriter()

            val writer = Rio.createWriter(outputFormat, sw)
                .setWriterConfig(WriterConfig()
                    .set(JSONLDSettings.JSONLD_MODE, mode))
            Rio.write(model, writer)

            return sw.toString()
        }
    }

}