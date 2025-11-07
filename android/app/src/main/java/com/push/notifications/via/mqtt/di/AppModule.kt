package com.push.notifications.via.mqtt.di

import com.push.notifications.via.mqtt.MqttManager
import com.push.notifications.via.mqtt.MqttViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Provide MqttManager as a singleton
    single { MqttManager(androidContext()) }

    // Provide MqttViewModel
    viewModel { MqttViewModel(get()) }
}
