package nl.tno.federated.api.security.user

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class SecurityUser (val user: UserEntity) : UserDetails {


    override fun getAuthorities(): List<SimpleGrantedAuthority> {
        val roles = user.roles
        val split = roles.split(",")
        val grants =  split.map{ it -> SimpleGrantedAuthority(it)}
        val returnList  =  grants.toList()
        return returnList
    }

    override fun getPassword(): String {
        return user.password
    }

    override fun getUsername(): String {
        return user.username
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return user.isEnabled
    }

}