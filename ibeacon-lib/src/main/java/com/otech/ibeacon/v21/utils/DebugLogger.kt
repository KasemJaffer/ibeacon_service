package com.otech.ibeacon.v21.utils

import android.util.Log


object DebugLogger {

    fun v(tag: String, text: String) = Log.v(tag, text)

    fun d(tag: String, text: String) = Log.d(tag, text)

    fun i(tag: String, text: String) = Log.i(tag, text)

    fun w(tag: String, text: String) = Log.w(tag, text)

    fun e(tag: String, text: String) = Log.e(tag, text)

    fun wtf(tag: String, text: String) = Log.wtf(tag, text)
}
