package com.push.notifications.via.mqtt

import android.app.Application
import com.push.notifications.via.mqtt.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MqttApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Start Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MqttApplication)
            modules(appModule)
        }
    }
}
