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

package nl.tno.federated.api.rml

import be.ugent.idlab.knows.functions.agent.AgentFactory
import be.ugent.rml.Executor
import be.ugent.rml.Utils
import be.ugent.rml.records.RecordsFactory
import be.ugent.rml.store.QuadStore
import be.ugent.rml.store.RDF4JStore
import be.ugent.rml.term.NamedNode
import org.eclipse.rdf4j.rio.RDFFormat
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.pathString

class RMLMapper {

    private val log = LoggerFactory.getLogger(RMLMapper::class.java)

    fun createTriples(data: String, rml: String): String? {
        val result = mapRml(data, rml)
        log.trace("Result: $result")
        return result
    }

    /**
     * Run rml mapper for the given json data and provided ttl mapping.
     */
    private fun mapRml(jsonData: String, rml: String): String? {
        // Replace all rml:source references to data.json since the RML mapper expects them in a file called data.json
        val rmlToUse = replaceBetween(input = rml, "rml:source", ";", " \"data.json\" ")

        // RmlMapper requires an unique folder per request, it read the data from data.json
        val tempDir = Files.createTempDirectory("semantic-adapter")
        log.debug("Created temp dir: ${tempDir.absolutePathString()}")

        val dataFile = Files.createFile(tempDir.resolve("data.json"))
        log.debug("Created data file: ${dataFile.absolutePathString()}")

        Files.write(dataFile, jsonData.toByteArray(StandardCharsets.UTF_8))

        try {
            // Load the mapping in a QuadStore
            val rmlStore = RDF4JStore()

            ByteArrayInputStream(rmlToUse.toByteArray(StandardCharsets.UTF_8)).use {
                rmlStore.read(it, null, RDFFormat.TURTLE)
            }

            // Set up the basepath for the records factory, i.e., the basepath for the (local file) data sources
            val factory = RecordsFactory(tempDir.pathString,tempDir.pathString)

            // Set up the outputstore (needed when you want to output something else than nquads
            val outputStore = RDF4JStore()

            // Create the Executor
            val executor = Executor(rmlStore, factory, outputStore, Utils.getBaseDirectiveTurtle(rmlToUse), functionAgent)

            // Execute the mapping
            log.info("Executing RML mapping...")
            val targets = executor.execute(null) ?: return null

            // Get result from store
            val result = targets[NamedNode("rmlmapper://default.store")] ?: return null
            result.copyNameSpaces(rmlStore)
            return writeToString(result)
        } finally {
            // Cleanup
            dataFile.deleteIfExists()
            log.debug("Deleted data file: ${dataFile.absolutePathString()}")
            tempDir.deleteIfExists()
            log.debug("Deleted temp dir: ${tempDir.absolutePathString()}")
        }
    }

    @Throws(Exception::class)
    private fun writeToString(store: QuadStore, format: String = "turtle"): String {
        // Default target is exported separately for backwards compatibility reasons
        log.debug("Exporting to default Target")

        if (store.size() > 1) {
            log.info("{} quads were generated for default Target", store.size())
        } else {
            log.info("{} quad was generated for default Target", store.size())
        }

        //if output file provided, write to triples output file
        val str = StringWriter()

        BufferedWriter(str).use {
            store.write(it, format)
        }

        return str.toString()
    }

    fun replaceBetween(input: String, start: String, end: String, replacement: String): String {
        var startIndex = input.indexOf(start)
        if (startIndex <= 0) return input

        var result = input

        while (startIndex > 0) {
            val endIndex = result.indexOf(end, startIndex = startIndex)
            log.info("Replacing value in string '${input.substring(startIndex, endIndex + end.length)}' between: '{}' and: '{}' with: '{}'", start, end, replacement)
            result = result.replaceRange(startIndex + start.length, endIndex, replacement)
            startIndex = result.indexOf(start, startIndex = endIndex)
        }

        return result
    }

    companion object {
        val functionAgent = AgentFactory.createFromFnO("fno/functions_idlab.ttl", "fno/functions_idlab_classes_java_mapping.ttl", "functions_grel.ttl", "grel_java_mapping.ttl");
    }
}