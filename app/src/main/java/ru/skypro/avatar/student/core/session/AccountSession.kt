package ru.skypro.avatar.student.core.session

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.ktor.client.HttpClient
import kotlinx.coroutines.cancel
import ru.skypro.avatar.student.core.network.createHttpClient

class AccountSession(persistence: SessionPersistence, onExpired: (AccountSession) -> Unit) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    val lease = SessionLease { onExpired(this) }
    val client: HttpClient = createHttpClient(SessionCookieStorage(persistence, lease), lease)

    fun close() {
        lease.revoke()
        viewModelStore.clear()
        client.coroutineContext.cancel()
        client.close()
    }
}
