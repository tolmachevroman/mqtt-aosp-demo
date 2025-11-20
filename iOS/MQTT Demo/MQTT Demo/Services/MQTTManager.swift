//
//  MQTTManager.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import Foundation
import Combine
import MQTTNIO
import NIO
import NIOTransportServices
import Logging

/// Service layer managing MQTT client operations
/// Provides async/await API for modern Swift concurrency
class MQTTManager: ObservableObject {
    
    // MARK: - Published Properties
    
    @Published var connectionState: MQTTConnectionState = .disconnected
    @Published var messages: [MQTTMessage] = []
    
    // MARK: - Private Properties
    
    private var client: MQTTClient?
    private var logger: Logger
    private var currentSubscriptions: Set<String> = []
    
    // MARK: - Configuration
    
    struct Configuration {
        var host: String = "localhost"
        var port: Int = 1883
        var keepAliveInterval: TimeAmount = .seconds(60)
        var cleanSession: Bool = true
        var clientID: String
        
        nonisolated init(
            host: String = "localhost",
            port: Int = 1883,
            keepAliveInterval: TimeAmount = .seconds(60),
            cleanSession: Bool = true,
            clientID: String = "iOS-\(UUID().uuidString.prefix(8))"
        ) {
            self.host = host
            self.port = port
            self.keepAliveInterval = keepAliveInterval
            self.cleanSession = cleanSession
            self.clientID = clientID
        }
    }
    
    // MARK: - Initialization
    
    nonisolated init() {
        var logger = Logger(label: "com.mqtt.demo")
        logger.logLevel = .info
        self.logger = logger
        
        logger.info("MQTTManager initialized")
    }
    
    deinit {
        // Clean up client resources
        if let client = client {
            _ = try? client.syncShutdownGracefully()
        }
    }
    
    // MARK: - Connection Management
    
    /// Connect to MQTT broker
    func connect(configuration: Configuration = Configuration()) async throws {
        guard !connectionState.isConnected else {
            logger.warning("Already connected")
            return
        }
        
        connectionState = .connecting
        logger.info("Connecting to \(configuration.host):\(configuration.port)")
        
        do {
            // Use NIOTransportServices EventLoopGroup for iOS (Network.framework)
            let client = MQTTClient(
                host: configuration.host,
                port: configuration.port,
                identifier: configuration.clientID,
                eventLoopGroupProvider: .shared(NIOTSEventLoopGroup.singleton),
                logger: logger
            )
            
            self.client = client
            
            // Connect with options
            try await client.connect(
                cleanSession: configuration.cleanSession
            )
            
            connectionState = .connected
            logger.info("Successfully connected to broker")
            
            // Set up message listener right after connection
            setupMessageListener()
            
        } catch {
            connectionState = .error(error.localizedDescription)
            logger.error("Connection failed: \(error.localizedDescription)")
            throw error
        }
    }
    
    /// Disconnect from MQTT broker
    func disconnect() async throws {
        guard let client = client, connectionState.isConnected else {
            return
        }
        
        connectionState = .disconnecting
        logger.info("Disconnecting from broker")
        
        do {
            try await client.disconnect()
            self.client = nil
            currentSubscriptions.removeAll()
            isListenerSetup = false
            connectionState = .disconnected
            logger.info("Successfully disconnected")
        } catch {
            connectionState = .error(error.localizedDescription)
            logger.error("Disconnect failed: \(error.localizedDescription)")
            throw error
        }
    }
    
    // MARK: - Subscribe
    
    /// Subscribe to a topic
    func subscribe(to topic: String, qos: MQTTQoS = .atLeastOnce) async throws {
        guard let client = client, connectionState.isConnected else {
            throw MQTTError.notConnected
        }
        
        logger.info("Subscribing to topic: \(topic) with QoS: \(qos.rawValue)")
        
        do {
            _ = try await client.subscribe(
                to: [
                    MQTTSubscribeInfo(topicFilter: topic, qos: qos)
                ]
            )
            
            currentSubscriptions.insert(topic)
            logger.info("Successfully subscribed to: \(topic)")
            
        } catch {
            logger.error("Subscribe failed: \(error.localizedDescription)")
            throw error
        }
    }
    
    /// Unsubscribe from a topic
    func unsubscribe(from topic: String) async throws {
        guard let client = client, connectionState.isConnected else {
            throw MQTTError.notConnected
        }
        
        logger.info("Unsubscribing from topic: \(topic)")
        
        do {
            try await client.unsubscribe(from: [topic])
            currentSubscriptions.remove(topic)
            logger.info("Successfully unsubscribed from: \(topic)")
        } catch {
            logger.error("Unsubscribe failed: \(error.localizedDescription)")
            throw error
        }
    }
    
    // MARK: - Publish
    
    /// Publish a message to a topic
    func publish(
        message: String,
        to topic: String,
        qos: MQTTQoS = .atLeastOnce,
        retain: Bool = false
    ) async throws {
        guard let client = client, connectionState.isConnected else {
            throw MQTTError.notConnected
        }
        
        logger.info("Publishing to topic: \(topic)")
        
        do {
            let payload = ByteBuffer(string: message)
            
            try await client.publish(
                to: topic,
                payload: payload,
                qos: qos,
                retain: retain
            )
            
            // Only add to messages if we're not subscribed to this topic
            // (if subscribed, we'll receive it via the listener)
            if !currentSubscriptions.contains(topic) {
                let mqttMessage = MQTTMessage(
                    topic: topic,
                    payload: message,
                    timestamp: Date(),
                    direction: .sent
                )
                
                DispatchQueue.main.async {
                    self.messages.append(mqttMessage)
                }
            }
            
            logger.info("Successfully published message")
            
        } catch {
            logger.error("Publish failed: \(error.localizedDescription)")
            throw error
        }
    }
    
    // MARK: - Message Listening
    
    private var isListenerSetup = false
    
    /// Set up message listener using callback
    private func setupMessageListener() {
        guard let client = client, !isListenerSetup else { return }
        
        isListenerSetup = true
        logger.info("Setting up message listener")
        
        // Set up publish listener callback
        client.addPublishListener(named: "main") { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .success(let publishInfo):
                let payload = String(buffer: publishInfo.payload)
                
                print("📩 Received message on \(publishInfo.topicName): \(payload)")
                self.logger.info("Received message on \(publishInfo.topicName): \(payload)")
                
                let mqttMessage = MQTTMessage(
                    topic: publishInfo.topicName,
                    payload: payload,
                    timestamp: Date(),
                    direction: .received
                )
                
                // Update on main thread for UI
                DispatchQueue.main.async {
                    print("📱 Adding message to UI: \(payload)")
                    self.messages.append(mqttMessage)
                    print("📊 Total messages: \(self.messages.count)")
                }
                
            case .failure(let error):
                print("❌ Error receiving message: \(error.localizedDescription)")
                self.logger.error("Error receiving message: \(error.localizedDescription)")
            }
        }
    }
    
    // MARK: - Helper Methods
    
    /// Check if a topic matches a subscription filter (supports wildcards)
    private func topicMatches(topic: String, filter: String) -> Bool {
        // Exact match
        if topic == filter {
            return true
        }
        
        let topicParts = topic.split(separator: "/")
        let filterParts = filter.split(separator: "/")
        
        // Multi-level wildcard (#)
        if filter.hasSuffix("#") {
            let filterPrefix = filterParts.dropLast()
            return topicParts.prefix(filterPrefix.count).elementsEqual(filterPrefix)
        }
        
        // Single-level wildcard (+)
        if filterParts.count != topicParts.count {
            return false
        }
        
        for (filterPart, topicPart) in zip(filterParts, topicParts) {
            if filterPart != "+" && filterPart != topicPart {
                return false
            }
        }
        
        return true
    }
    
    /// Clear all messages
    func clearMessages() {
        messages.removeAll()
        logger.info("Messages cleared")
    }
}

// MARK: - Error Types

enum MQTTError: LocalizedError {
    case notConnected
    case invalidConfiguration
    case connectionFailed(String)
    
    var errorDescription: String? {
        switch self {
        case .notConnected:
            return "Not connected to MQTT broker"
        case .invalidConfiguration:
            return "Invalid MQTT configuration"
        case .connectionFailed(let reason):
            return "Connection failed: \(reason)"
        }
    }
}
