package nextapp.saftest

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SAFTest(
    private val context: Context,
    private val rootUri: Uri
) : BaseTest() {

    @Suppress("unused")
    object Projection {

        val COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        const val INDEX_DOCUMENT_ID = 0
        const val INDEX_MIME_TYPE = 1
        const val INDEX_SIZE = 2
    }

    override suspend fun execute() {
        startClock()
        val rootId = DocumentsContract.getTreeDocumentId(rootUri)
        val treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, rootId)
        withContext(Dispatchers.IO) {
            scan(context, treeUri)
        }
        stopClock()
    }

    private suspend fun scan(context: Context, childTreeUri: Uri) {
        val cursor = context.contentResolver.query(
            childTreeUri, Projection.COLUMNS, null, null, null)
        cursor ?: return

        val childFolderUriList = mutableListOf<Uri>()
        while (cursor.moveToNext()) {
            val documentId = cursor.getString(Projection.INDEX_DOCUMENT_ID)
            val mimeType = cursor.getString(Projection.INDEX_MIME_TYPE)
            val isDirectory= DocumentsContract.Document.MIME_TYPE_DIR == mimeType

            if (isDirectory) {
                childFolderUriList += DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, documentId)
                foldersScanned++
            } else {
                val size = cursor.getLong(Projection.INDEX_SIZE)
                totalSize += size
                filesScanned++
            }

            //Log.d("TEST", "--$name")
        }
        cursor.close()

        childFolderUriList.forEach { scan(context, it) }
    }
}