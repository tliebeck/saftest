package nextapp.saftest

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.layout_test.view.*
import java.text.NumberFormat

class TestView(context: Context, attrs: AttributeSet? = null): LinearLayout(context, attrs) {

    private val numberFormat = NumberFormat.getNumberInstance()

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.layout_test, this)
    }

    fun update(test: BaseTest) {
        val fileFormat = numberFormat.format(test.filesScanned)
        val folderFormat = numberFormat.format(test.foldersScanned)
        val countText = "$fileFormat / $folderFormat"

        countView.text = countText
        sizeView.text = numberFormat.format(test.totalSize)
        timeView.text = numberFormat.format(test.runTime)
    }
}