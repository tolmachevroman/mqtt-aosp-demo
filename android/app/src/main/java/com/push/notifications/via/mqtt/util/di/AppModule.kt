package com.push.notifications.via.mqtt.util.di

import com.mqtt.core.data.repository.MqttRepository
import com.push.notifications.via.mqtt.ui.screens.MqttViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { MqttRepository(androidContext()) }
    viewModel { MqttViewModel(get()) }
}
