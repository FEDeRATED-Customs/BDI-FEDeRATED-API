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
package nl.tno.federated.api.security.user

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*


enum class Roles(@JsonValue val role: String) {
    API_USER ("API_USER"),
    API_EVENT ("API_EVENT"),
    API_MESSAGE ("API_MESSAGE"),
    API_ADMIN ("API_ADMIN")
}

@Entity
@Table(name = "API_USERS")
data class UserEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "USERNAME")
    var username: String,
    @Column(name = "PASSWORD")
    var password: String,
    @Column(name = "ROLES")
    var roles: String = "API_USER",
    @Column(name = "ISENABLED")
    var isEnabled: Boolean = true,
)

