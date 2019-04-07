package nextapp.saftest

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.*
import java.text.NumberFormat
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        const val DOCUMENT_REQUEST_CODE = 1
        const val PERMISSIONS_REQUEST_CODE = 2
        const val SAF_URI = "safUri"
        const val MONITOR_UPDATE_INTERVAL = 500L
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var pref: SharedPreferences
    private lateinit var job: Job
    private var uri: Uri? = null

    private fun monitorTest(testView: TestView, test: BaseTest) = launch {
        while (true) {
            testView.update(test)
            delay(MONITOR_UPDATE_INTERVAL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            DOCUMENT_REQUEST_CODE -> {
                setUri(data?.data, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        job = Job()
        pref = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        selectRoot.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), DOCUMENT_REQUEST_CODE)
        }

        fab.setOnClickListener { runTest() }

        setUri(pref.getString(SAF_URI, null)?.let { Uri.parse(it) }, false)

        setupPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun runTest() = launch {
        val safUri = uri
        if (safUri == null) {
            AlertDialog.Builder(this@MainActivity)
                .setMessage("SAF target must be set before starting test.")
                .setPositiveButton("OK", null)
                .show()
            return@launch
        }

        if (!setupPermissions()) {
            return@launch
        }

        resultView.text = getString(R.string.result_in_progress)

        fab.isEnabled = false

        val fileTest = FileTest(Environment.getExternalStorageDirectory())
        fileTestView.update(fileTest)

        val safTest = SAFTest(this@MainActivity, safUri)
        safTestView.update(safTest)

        val fileMonitor = monitorTest(fileTestView, fileTest)
        try {
            fileTest.execute()
        } finally {
            fileMonitor.cancel()
        }
        fileTestView.update(fileTest)

        val safMonitor = monitorTest(safTestView, safTest)
        try {
            safTest.execute()
        } finally {
            safMonitor.cancel()
        }
        safTestView.update(safTest)

        fab.isEnabled = true

        if (Math.round(safTest.totalSize / 1000f) != Math.round(fileTest.totalSize / 1000f)) {
            resultView.text = getString(R.string.result_warning_size_delta)
        } else {
            val percent = NumberFormat.getPercentInstance()
                .format(safTest.runTime.toFloat() / fileTest.runTime.toFloat())
            resultView.text = getString(R.string.result_format, percent)
        }
    }

    private fun setUri(uri: Uri?, persist: Boolean) {
        this.uri = uri
        if (uri == null) {
            safUri.text = getString(R.string.value_no_uri)
            if (persist) {
                pref.edit().remove(SAF_URI).apply()
            }
        } else {
            val uriString = uri.toString()
            safUri.text = uriString
            if (persist) {
                contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                pref.edit().putString(SAF_URI, uriString).apply()
            }
        }
    }

    private fun setupPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_CODE)
            false
        } else {
            true
        }
    }
}
