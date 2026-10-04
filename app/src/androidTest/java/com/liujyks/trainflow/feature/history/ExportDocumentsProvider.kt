package com.liujyks.trainflow.feature.history

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/** Two fixed roots backed only by this test APK's private files. */
class ExportDocumentsProvider : DocumentsProvider() {
    private val rootColumns = arrayOf(Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE,
        Root.COLUMN_FLAGS, Root.COLUMN_MIME_TYPES, Root.COLUMN_AVAILABLE_BYTES)
    private val documentColumns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED)
    private val exportDirectory get() = File(requireNotNull(context).filesDir, "e21-s04-documents")
    override fun onCreate(): Boolean = true
    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        if (parentDocumentId != "success" && parentDocumentId != "failure") return false
        val parts = documentId.split('/')
        if (parts.size != 2 || parts[0] != parentDocumentId) return false
        val directory = File(exportDirectory, parentDocumentId)
        val file = File(directory, parts[1])
        return file.parentFile == directory && file.isFile && file.extension == "json" &&
            File(directory, file.name + ".meta").isFile
    }
    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(projection ?: rootColumns).apply {
        for ((id, title) in listOf("success" to "E21-S04 成功目标", "failure" to "E21-S04 失败目标")) {
            newRow().add(Root.COLUMN_ROOT_ID, id).add(Root.COLUMN_DOCUMENT_ID, id).add(Root.COLUMN_TITLE, title)
                .add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE).add(Root.COLUMN_MIME_TYPES, "application/json")
                .add(Root.COLUMN_AVAILABLE_BYTES, requireNotNull(context).filesDir.usableSpace)
        }
    }
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: documentColumns).apply { addDocument(documentId) }
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor =
        MatrixCursor(projection ?: documentColumns).apply {
            require(parentDocumentId == "success" || parentDocumentId == "failure")
            val directory = File(exportDirectory, parentDocumentId)
            if (directory.exists()) requireNotNull(directory.listFiles()).filter { it.extension == "json" }.forEach {
                addDocument("$parentDocumentId/${it.name}")
            }
        }
    private fun MatrixCursor.addDocument(id: String) {
        val directory = id == "success" || id == "failure"
        val file = File(exportDirectory, id)
        newRow().add(Document.COLUMN_DOCUMENT_ID, id)
            .add(Document.COLUMN_DISPLAY_NAME, if (directory) if (id == "success") "E21-S04 成功目标" else "E21-S04 失败目标"
                else JSONObject(File(file.parentFile, file.name + ".meta").readText()).getString("displayName"))
            .add(Document.COLUMN_MIME_TYPE, if (directory) Document.MIME_TYPE_DIR else "application/json")
            .add(Document.COLUMN_FLAGS, if (directory) Document.FLAG_DIR_SUPPORTS_CREATE else 0)
            .add(Document.COLUMN_SIZE, if (directory) 0 else file.length())
            .add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
    }
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        require(parentDocumentId == "success" || parentDocumentId == "failure")
        require(mimeType == "application/json")
        val directory = File(exportDirectory, parentDocumentId)
        check(directory.mkdirs() || directory.isDirectory)
        val file = File(directory, "${UUID.randomUUID()}.json")
        check(file.createNewFile())
        File(directory, file.name + ".meta").writeText(JSONObject().put("displayName", displayName).toString())
        val id = "$parentDocumentId/${file.name}"
        ledger(JSONObject().put("event", "create").put("documentId", id).put("displayName", displayName))
        return id
    }
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = File(exportDirectory, documentId)
        val writing = mode.contains('w')
        val record = JSONObject().put("event", "open").put("documentId", documentId).put("mode", mode)
            .put("relativePath", "files/e21-s04-documents/$documentId")
        if (documentId.startsWith("failure/") && writing) {
            ledger(record.put("failure", "E21-S04 fixed open failure"))
            throw FileNotFoundException("E21-S04 fixed open failure")
        }
        if (!writing) {
            record.put("size", file.length()).put("sha256", MessageDigest.getInstance("SHA-256")
                .digest(file.readBytes()).joinToString("") { "%02x".format(it) })
        }
        ledger(record)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }
    private fun ledger(event: JSONObject) {
        check(exportDirectory.mkdirs() || exportDirectory.isDirectory)
        File(exportDirectory, "ledger.jsonl").appendText(event.toString() + "\n", Charsets.UTF_8)
    }
}
