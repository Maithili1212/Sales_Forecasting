package com.smsms.app

import android.app.Application
import com.smsms.app.auth.AuthManager

class SmsmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
    }
}
