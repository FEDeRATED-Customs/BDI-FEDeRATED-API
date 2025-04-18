package nl.tno.federated.api.security.user

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService ( private val userRepository: UserRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(UserService::class.java)
        private var objectMapper = jacksonObjectMapper()
        private val encoder = BCryptPasswordEncoder()
    }

    fun findUser(username: String) : User? {
        return userRepository.findByUsername(username)?.let{ it -> User(it.username, "************", it.roles, it.isEnabled)}
    }

    fun addUser(user: User) {
        userRepository.findByUsername(user.username)?.let { throw UserManagementException("Username '${user.username}' is allready in use ") }
        userRepository.save(UserEntity(username = user.username, password = encoder.encode(user.password), roles = user.roles, isEnabled = user.isEnabled))
    }

    fun updateUser(username: String, user: User) {
        userRepository.findByUsername(username)?.let { it ->
            if (it.username != user.username)  it.username = user.username
            if (it.roles != user.roles)  it.roles = user.roles
            if (it.isEnabled != user.isEnabled)  it.isEnabled = user.isEnabled
            userRepository.save(it)
        }
    }

    fun updatePassword(username: String, password: String) {
        userRepository.findByUsername(username)?.let { it ->
            it.password = encoder.encode(password)
            userRepository.save(it)
        }
    }

    fun disableUser(username: String) {
        userRepository.findByUsername(username)?.let { it ->
            it.isEnabled = false
            userRepository.save(it)
        }
    }

    fun getUsers(): List<User> {
        return userRepository.findAll().map{it -> User(it.username, "************", it.roles , it.isEnabled)}
    }
}