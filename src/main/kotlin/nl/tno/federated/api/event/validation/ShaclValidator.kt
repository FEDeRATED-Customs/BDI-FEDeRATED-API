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

package nl.tno.federated.api.event.validation

import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.vocabulary.RDF4J
import org.eclipse.rdf4j.repository.RepositoryException
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.WriterConfig
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.sail.shacl.ShaclSail
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException
import org.slf4j.LoggerFactory

import java.io.StringReader
import java.io.StringWriter


class ShaclValidationException(msg: String?) : Exception(msg)

/**
 * https://rdf4j.org/documentation/programming/shacl/
 */
class ShaclValidator(private val shapes: List<String>) {

    private val log = LoggerFactory.getLogger(ShaclValidator::class.java)

    /**
     * Add all the shapes to the SailRepository.
     * Do this only once, not per validate action.
     */
    private fun addShapes(sailRepository: SailRepository) {
        shapes.forEach {
            sailRepository.connection.use { connection ->
                connection.begin()
                connection.add(StringReader(it), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH)
                connection.commit()
            }
        }
    }

    private fun initRepository(): SailRepository {
        val shaclSail = ShaclSail(MemoryStore())
        val sailRepository = SailRepository(shaclSail)
        sailRepository.init()
        addShapes(sailRepository)
        return sailRepository
    }

    fun validate(rdf: String) {
        val sailRepository = initRepository()

        try {
            sailRepository.connection.use { connection ->
                connection.begin()
                connection.add(StringReader(rdf), "", RDFFormat.TURTLE)
                try {
                    connection.commit()
                } catch (exception: RepositoryException) {
                    val cause: Throwable? = exception.cause
                    if (cause is ShaclSailValidationException) {
                        val validationReportModel: Model = cause.validationReportAsModel()
                        val writerConfig: WriterConfig = WriterConfig()
                            .set(BasicWriterSettings.INLINE_BLANK_NODES, true)
                            .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
                            .set(BasicWriterSettings.PRETTY_PRINT, true)
                        val sw = StringWriter()
                        Rio.write(validationReportModel, sw, RDFFormat.TURTLE, writerConfig)
                        log.debug("SHACL validation failed, validation report:\n {}", sw.toString())
                        throw (ShaclValidationException(cause.message))
                    }
                    throw exception
                }
            }
        } finally {
            sailRepository.shutDown()
        }
    }
}