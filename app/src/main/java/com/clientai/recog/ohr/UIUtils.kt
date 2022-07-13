package com.clientai.recog.ohr

import java.text.DecimalFormat

object UIUtils {
    var density = 1.0f

    fun dp2px(dp: Float): Int {
        return (dp * density + 0.5f).toInt()
    }

    fun formatDisplayFloat(number: Float): String {
        val format = DecimalFormat("0.##")
        return format.format(number)
    }
}