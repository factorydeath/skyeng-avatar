package ru.skypro.avatar.student.core.session

import io.ktor.client.HttpClient
import io.ktor.util.AttributeKey

class SessionLease(private val onExpired: () -> Unit = {}) {
    private var active = true
    private var authenticated = false

    @Synchronized
    fun <T> whileActive(block: () -> T): T? = if (active) block() else null

    @Synchronized
    fun authenticate() {
        if (active) authenticated = true
    }

    @Synchronized
    fun revoke() {
        active = false
    }

    fun unauthorized() {
        val notify = synchronized(this) {
            if (!active || !authenticated) false else {
                active = false
                true
            }
        }
        if (notify) onExpired()
    }
}

val SessionLeaseKey = AttributeKey<SessionLease>("avatar.session.lease")

fun HttpClient.notifySessionExpired() {
    attributes.getOrNull(SessionLeaseKey)?.unauthorized()
}
