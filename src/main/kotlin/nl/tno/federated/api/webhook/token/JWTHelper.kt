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