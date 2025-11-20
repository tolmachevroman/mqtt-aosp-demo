//
//  MQTTMessage.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import Foundation

/// Represents a single MQTT message in the UI
struct MQTTMessage: Identifiable, Equatable {
    let id = UUID()
    let topic: String
    let payload: String
    let timestamp: Date
    let direction: MessageDirection
    
    enum MessageDirection {
        case sent
        case received
    }
    
    var formattedTime: String {
        let formatter = DateFormatter()
        formatter.timeStyle = .medium
        return formatter.string(from: timestamp)
    }
}
