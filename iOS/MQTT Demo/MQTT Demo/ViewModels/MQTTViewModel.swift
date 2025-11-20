//
//  MQTTViewModel.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import Foundation
import SwiftUI
import Combine

/// ViewModel managing MQTT state and user interactions
@MainActor
class MQTTViewModel: ObservableObject {
    
    // MARK: - Published Properties
    
    @Published var brokerHost: String = "localhost"
    @Published var brokerPort: String = "1883"
    @Published var subscribeTopic: String = "demo/messages"
    @Published var publishTopic: String = "demo/messages"
    @Published var messageToSend: String = ""
    @Published var statusMessage: String = "Ready to connect"
    @Published var showError: Bool = false
    @Published var errorMessage: String = ""
    @Published var messages: [MQTTMessage] = []
    
    // MARK: - Dependencies
    
    @ObservedObject var mqttManager: MQTTManager
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Computed Properties
    
    var isConnected: Bool {
        mqttManager.connectionState.isConnected
    }
    
    var connectionState: MQTTConnectionState {
        mqttManager.connectionState
    }
    
    var canConnect: Bool {
        !brokerHost.isEmpty && !brokerPort.isEmpty && !isConnected
    }
    
    var canSubscribe: Bool {
        isConnected && !subscribeTopic.isEmpty
    }
    
    var canPublish: Bool {
        isConnected && !publishTopic.isEmpty && !messageToSend.isEmpty
    }
    
    // MARK: - Initialization
    
    init(mqttManager: MQTTManager = MQTTManager()) {
        self.mqttManager = mqttManager
        
        // Sync messages from MQTTManager to ViewModel
        mqttManager.$messages
            .receive(on: DispatchQueue.main)
            .assign(to: &$messages)
    }
    
    // MARK: - Connection Actions
    
    /// Connect to MQTT broker
    func connect() {
        guard canConnect else { return }
        
        guard let port = Int(brokerPort) else {
            showErrorAlert("Invalid port number")
            return
        }
        
        statusMessage = "Connecting to \(brokerHost):\(port)..."
        
        Task {
            do {
                let config = MQTTManager.Configuration(
                    host: brokerHost,
                    port: port
                )
                
                try await mqttManager.connect(configuration: config)
                statusMessage = "Connected successfully!"
                
            } catch {
                statusMessage = "Connection failed"
                showErrorAlert(error.localizedDescription)
            }
        }
    }
    
    /// Disconnect from MQTT broker
    func disconnect() {
        guard isConnected else { return }
        
        statusMessage = "Disconnecting..."
        
        Task {
            do {
                try await mqttManager.disconnect()
                statusMessage = "Disconnected"
            } catch {
                showErrorAlert(error.localizedDescription)
            }
        }
    }
    
    // MARK: - Subscription Actions
    
    /// Subscribe to a topic
    func subscribe() {
        guard canSubscribe else { return }
        
        statusMessage = "Subscribing to \(subscribeTopic)..."
        
        Task {
            do {
                try await mqttManager.subscribe(to: subscribeTopic)
                statusMessage = "Subscribed to \(subscribeTopic)"
            } catch {
                statusMessage = "Subscription failed"
                showErrorAlert(error.localizedDescription)
            }
        }
    }
    
    // MARK: - Publish Actions
    
    /// Publish a message to a topic
    func publishMessage() {
        guard canPublish else { return }
        
        let topic = publishTopic
        let message = messageToSend
        
        statusMessage = "Publishing to \(topic)..."
        
        Task {
            do {
                try await mqttManager.publish(
                    message: message,
                    to: topic
                )
                
                statusMessage = "Message published successfully"
                
                // Clear the message input after successful publish
                messageToSend = ""
                
            } catch {
                statusMessage = "Publish failed"
                showErrorAlert(error.localizedDescription)
            }
        }
    }
    
    // MARK: - Message Management
    
    /// Clear all messages
    func clearMessages() {
        mqttManager.clearMessages()
        statusMessage = "Messages cleared"
    }
    
    // MARK: - Helper Methods
    
    private func showErrorAlert(_ message: String) {
        errorMessage = message
        showError = true
    }
}
