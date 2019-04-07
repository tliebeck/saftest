package nextapp.saftest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileTest (
    private val rootFile: File
) : BaseTest() {

    override suspend fun execute() {
        startClock()
        withContext(Dispatchers.IO) {
            scan(rootFile)
        }
        stopClock()
    }

    private suspend fun scan(file: File) {
        val children = file.listFiles()

        for (child in children) {
            if (child.isDirectory) {
                scan(child)
                foldersScanned++
            } else {
                totalSize += child.length()
                filesScanned++
            }
            //Log.d("TEST", "--${child.name}")
        }
    }
}