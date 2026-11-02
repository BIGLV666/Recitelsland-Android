package com.biglv.recitelsland

import android.app.Application

class ReciteApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
