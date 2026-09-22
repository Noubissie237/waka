package com.propentatech.waka

import android.app.Application

class WakaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
