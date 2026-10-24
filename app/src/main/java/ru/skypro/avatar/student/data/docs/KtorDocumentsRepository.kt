package ru.skypro.avatar.student.data.docs

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.core.network.ApiEndpoints
import ru.skypro.avatar.student.core.network.decodeOrThrow
import ru.skypro.avatar.student.core.network.getDecoded
import ru.skypro.avatar.student.core.network.dto.DocumentDownloadDto
import ru.skypro.avatar.student.core.network.dto.DocumentsResponseDto
import ru.skypro.avatar.student.domain.docs.StudentDocument

class KtorDocumentsRepository(private val client: HttpClient) {
    fun documents(): Flow<List<StudentDocument>> = flow {
        AppLog.d(AppLog.DOCS, "documents start")
        try {
            val dto: DocumentsResponseDto = client.getDecoded(ApiEndpoints.DOCUMENTS, AppLog.DOCS, "documents")
            AppLog.i(AppLog.DOCS, "documents ok n=${dto.documents.size}")
            emit(dto.documents.map { StudentDocument(it.id, it.number, it.createdAt) })
        } catch (e: Exception) {
            AppLog.e(AppLog.DOCS, "documents failed", e)
            throw e
        }
    }

    suspend fun signedUrl(id: Long): String {
        AppLog.d(AppLog.DOCS, "download id=$id")
        return try {
            val dto: DocumentDownloadDto = client.getDecoded("${ApiEndpoints.DOCUMENTS}/$id/download", AppLog.DOCS, "download")
            AppLog.i(AppLog.DOCS, "download ok len=${dto.signedUrl.length}")
            dto.signedUrl
        } catch (e: Exception) {
            AppLog.e(AppLog.DOCS, "download failed id=$id", e)
            throw e
        }
    }
}
