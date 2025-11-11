package com.push.notifications.via.mqtt.di

import com.mqtt.core.repository.MqttRepository
import com.push.notifications.via.mqtt.MqttViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { MqttRepository(androidContext()) }
    viewModel { MqttViewModel(get()) }
}
