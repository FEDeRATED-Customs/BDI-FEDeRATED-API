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