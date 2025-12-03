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

package nl.tno.federated.api.graphdb

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.tno.federated.api.graphdb.config.GraphDBConfig
import nl.tno.federated.api.graphdb.PrefixHandlerQueries
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Exception thrown when anything happens in the GraphDBService
 */
open class GraphDBException(message: String) : Exception(message)

/**
 * Exception that might occur when the GraphDB server returns an exception (500..599)
 */
class GraphDBClientException(message: String) : GraphDBException(message)

/**
 * Exception that might occur when an invalid request is made to the GraphDB server (400..499)
 */
class GraphDBServerException(message: String) : GraphDBException(message)

@Service
class GraphDBService (private val config: GraphDBConfig): IGraphDBService {

    private val log = LoggerFactory.getLogger(GraphDBService::class.java)

    /**
     * See: https://www.baeldung.com/httpclient-timeout
     * And: https://www.baeldung.com/httpclient-connection-management
     */
    private val client: HttpClient by lazy {
         HttpClientBuilder.create()
            .setMaxConnPerRoute(config.triplestore.maxConnectionsPerRoute)
            .setMaxConnTotal(config.triplestore.maxConnectionsTotal)
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectTimeout(config.triplestore.connectTimeoutMillis)
                    .setConnectionRequestTimeout(config.triplestore.connectTimeoutMillis)
                    .setSocketTimeout(config.triplestore.socketTimeoutMillis)
                    .build()
            )
            .build()
    }


    override fun queryEventIds(): String {
        val sparql = """
            ${PrefixHandlerQueries.getPrefixesEvent()}
            select ?s where { 
                ?s a Event:Event
            }   
        """.trimIndent()
        return performSparql(sparql) ?: ""
    }

    override fun generalSPARQLquery(query: String): String {
        return performSparql(query.trimIndent()) ?: ""
    }

    override fun queryEventById(id: String): String {
        assertSPARQLInput(id)

        assertSPARQLInput(id)

        val sparql = """
            ${PrefixHandlerQueries.getPrefixesEvent()}
            select ?s where {
            ?s a Event:Event .
            FILTER regex (STR(?s), "$id")
            }
            """.trimIndent()
        return performSparql(sparql) ?: ""
    }

    override fun queryAllEventPropertiesById(id: String): String {
        val sparql = """
            ${PrefixHandlerQueries.getPrefixesEvent()}
            ${PrefixHandlerQueries.getPrefixesDigitalTwin()}
            SELECT DISTINCT ?subject ?object ?object1 ?object2
	        WHERE {
                ?subject a Event:Event .
                ?subject Event:involvesBusinessTransaction ?object .
                ?subject Event:involvesDigitalTwin ?object1, ?object2 .
                ?object1 a dt:Equipment .
                ?object2 a dt:TransportMeans .
                FILTER regex (STR(?subject), "$id")
            }
            """.trimIndent()
        return performSparql(sparql) ?: ""
    }

    override fun queryEventComponent(id: String): String {
        val sparql = """
            ${PrefixHandlerQueries.getPrefixesSemanticElements()}
            SELECT ?subject
            WHERE {
                ?subject a owl:NamedIndividual
                FILTER regex (STR(?subject), "$id")
            }
            """.trimIndent()
        return performSparql(sparql) ?: ""
    }

    override fun isQueryResultEmpty(queryResult: String): Boolean {
        val mapper = jacksonObjectMapper().readTree(queryResult)

        val queryBindings = mapper["results"]["bindings"].elements()

        return !queryBindings.hasNext()
    }

    override fun insertEvent(ttl: String): Boolean {
        val uri = getRepositoryURI()
        val result = client.post(URI("$uri/statements"), ttl)
        log.info("Insert into GraphDB, statusCode: {}, responseBody: {}", result.statusLine.statusCode, result.bodyAsString)
        return result.bodyAsString.isNullOrEmpty()
    }

    private fun performSparql(sparql: String): String? {
        val uri = getRepositoryURI()
        return client.get(URI("$uri?query=${URLEncoder.encode(sparql, UTF_8.toString())}"))?.bodyAsString
    }

    private fun getRepositoryURI(): String {
        return "${config.triplestore.protocol}://${config.triplestore.host}:${config.triplestore.port}/repositories/${config.triplestore.repository}"
    }


    private fun getInputStreamFromClassPathResource(filename: String): InputStream? {
        val file = Paths.get(filename)
        if (Files.exists(file)) {
            log.info("Using file: {}", file.toAbsolutePath())
            return Files.newInputStream(file)
        }
        log.info("Using classpath resource: {}", filename)
        return Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
    }

    /**
     * Check for possible to S(PAR)QL injection.
     */
    private fun assertSPARQLInput(str: String) {
        val suspiciousChars = listOf('?', '{', '}', ':')
        if (str.any { it in suspiciousChars }) {
            throw GraphDBException("Suspicious character detected in SPARQL input.")
        }
    }
}

private val HttpEntity.contentAsString: String
    get() = String(content.readBytes(), UTF_8)

private fun HttpClient.get(uri: URI): HttpResponse? {
    val request = HttpGet(uri).apply {
        setHeader("Accept", "application/json")
    }
    return execute(request)
}

private fun HttpClient.post(uri: URI, body: String): HttpResponse {
    return post(
        uri, StringEntity(body), mapOf(
        "Accept" to "application/json",
        "Content-Type" to "text/turtle"
    )
    )
}

private fun HttpClient.post(uri: URI, entity: HttpEntity, headers: Map<String, String>): HttpResponse {
    val request = HttpPost(uri).apply {
        this.entity = entity
        headers.forEach { (key, value) ->
            setHeader(key, value)
        }
    }
    return execute(request)
}

private val HttpResponse.bodyAsString: String?
    get() {
        return when (statusLine.statusCode) {
            in 400..499 -> throw GraphDBClientException(entity?.contentAsString ?: "Bad request when accessing GraphDB")
            in 500..599 -> throw GraphDBServerException(entity?.contentAsString ?: "Internal server error when accessing GraphDB")
            else -> entity?.contentAsString
        }
    }
