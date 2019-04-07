package nextapp.saftest

import android.os.SystemClock

abstract class BaseTest {

    var filesScanned = 0
    var foldersScanned = 0
    var totalSize = 0L

    private var startTime = 0L
    private var endTime = 0L

    val runTime: Long
        get() {
            if (startTime == 0L) {
                return 0L
            }
            if (endTime == 0L) {
                return SystemClock.elapsedRealtime() - startTime
            }
            return endTime - startTime
        }

    fun startClock() {
        endTime = 0
        startTime = SystemClock.elapsedRealtime()
    }

    fun stopClock() {
        endTime = SystemClock.elapsedRealtime()
    }

    abstract suspend fun execute()
}
