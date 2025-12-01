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

package nl.tno.federated.api.graphdb.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "federated.node.graphdb")
class GraphDBConfig (val triplestore : Triplestore) {
    class Triplestore {
        lateinit var protocol: String
        lateinit var host: String
        var port: Int = 0
        lateinit var repository: String
        var connectTimeoutMillis: Int = 0
        var socketTimeoutMillis: Int = 0
        var maxConnectionsPerRoute: Int = 0
        var maxConnectionsTotal: Int = 0
    }

    fun toConnectURL(): String {
        return triplestore.protocol + "://" + triplestore.host + ":" + triplestore.port + "/repositories/" + triplestore.repository
    }

    fun toUpdateURL(): String {
        return triplestore.protocol + "://" + triplestore.host + ":" + triplestore.port + "/repositories/" + triplestore.repository + "/statements"
    }
}