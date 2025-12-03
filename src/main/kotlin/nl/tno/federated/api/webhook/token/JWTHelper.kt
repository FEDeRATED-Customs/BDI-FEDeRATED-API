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

package nl.tno.federated.api.webhook.token

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import kotlin.io.path.Path
import kotlin.text.Charsets.UTF_8

@Service
object JwtHelper {
    /**
     * Enum class representing JWT algorithms with their corresponding values.
     */

    @Autowired
    lateinit var environment: Environment

    enum class JwtAlgorithm(val value: String) {
        ALGORITHM_HS256("HS256"), ALGORITHM_HS384("HS384"), ALGORITHM_HS512("HS512"), ALGORITHM_RS256("RS256")
    }

    /**
     * Enum class representing HMAC signature algorithms with their corresponding values.
     */
    private enum class SignatureAlgorithm(val value: String) {
        ALGORITHM_HS256("HmacSHA256"), ALGORITHM_HS384("HmacSHA384"), ALGORITHM_HS512("HmacSHA512"), ALGORITHM_RS256("RS256")
    }

    // Secret key for JWT generation and verification
    private var SECRET_KEY: String = ""
    // Default expiration time for JWT tokens (365 days)
    private val EXPIRATION_TIME_MS = 31536000000 // 365 days - Customizable

    // Default JWT algorithm and signature algorithm
    private var jwtAlgorithm: String = JwtAlgorithm.ALGORITHM_RS256.value
    private var signatureAlgorithm: String = SignatureAlgorithm.ALGORITHM_RS256.value

    /**
     * Initializes the JWT Helper with the secret key and algorithm.
     */
    fun init() {

    }

    /**
     * Checks if a JWT token has expired.
     *
     * @param token JWT token to be checked.
     * @return True if the token has expired, false otherwise.
     */
    fun isTokenExpired(token: String): Boolean {
        val payloadMap = extractPayload(token) ?: return true
        val expiration = payloadMap["exp"] as? Long ?: return true
        return System.currentTimeMillis() > expiration
    }

    /**
     * Decodes a base64 URL-encoded string.
     *
     * @param input Base64 URL-encoded string to decode.
     * @return Decoded string.
     */
    private fun decodeBase64URL(input: String): String {
        val decodedBytes = Base64.getUrlDecoder().decode(input)
        return String(decodedBytes, UTF_8)
    }

    /**
     * Extracts and decodes the payload from a JWT token.
     *
     * @param token JWT token from which to extract the payload.
     * @return Decoded payload as a Map or null if the token format is invalid.
     */
    fun extractPayload(token: String): Map<String, Any>? {
        val parts = token.split("\\.".toRegex())
        if (parts.size != 3) {
            return null
        }
        val payloadBase64 = parts[1]
        val payloadJson = String(Base64.getUrlDecoder().decode(payloadBase64), UTF_8)
        return ObjectMapper().readValue(payloadJson, Map::class.java) as Map<String, Any>
    }

    /**
     * Encodes a byte array into a base64 URL-encoded string and removes padding.
     *
     * @param input Byte array to encode.
     * @return Base64 URL-encoded string without padding.
     */
    private fun encodeBase64URL(input: ByteArray): String {
        val encoded = Base64.getUrlEncoder().encodeToString(input)
        return encoded.replace("=", "")
    }

    /**
     * Serializes a Map of key-value pairs into a JSON-formatted string.
     *
     * @param data Map of key-value pairs to be serialized.
     * @return JSON-formatted string representing the serialized data.
     */
    private fun serializeToJson(data: Map<String, Any>): String {
        val entries = data.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        return "{$entries}"
    }


    @Throws(java.lang.Exception::class)
    fun readRSAPrivateKeyFromProperties(environmentKey: String): RSAPrivateKey {
        val key = Base64.getDecoder().decode(environment.getProperty(environmentKey)).toString()
        return readRSAPrivateKey(key)
    }

    fun readRSAPrivateKey(key: String): RSAPrivateKey {
        val privateKeyPEM = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END PRIVATE KEY-----", "")
        val encoded: ByteArray = Base64.getDecoder().decode(privateKeyPEM)
        val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }


    fun createJWT(clientId: String, audience: String): String {
        return createJWT(clientId, audience,
            environment.getProperty("federated.node.api.webhook.privatekey")?.let { readRSAPrivateKey(it) })
    }

    fun createJWT(clientId: String, audience: String, key: RSAPrivateKey?): String {

        return  Jwts.builder()
                .setHeaderParam("alg", "RS256").setHeaderParam("typ", "JWT")
                .setIssuer(clientId)
                .setSubject(UUID.randomUUID().toString())
                .setAudience(audience)
                .setIssuedAt(Date())
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.RS256)
                .compact()
    }

}