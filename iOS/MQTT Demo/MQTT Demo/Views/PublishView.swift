//
//  PublishView.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import SwiftUI

/// View for publishing messages to MQTT topics
struct PublishView: View {
    @ObservedObject var viewModel: MQTTViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Publish Message")
                .font(.headline)
            
            // Topic Input
            VStack(alignment: .leading, spacing: 4) {
                Text("Topic")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                TextField("demo/messages", text: $viewModel.publishTopic)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                    .disabled(!viewModel.isConnected)
            }
            
            // Message Input
            VStack(alignment: .leading, spacing: 4) {
                Text("Message")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack(alignment: .bottom) {
                    TextField("Enter your message...", text: $viewModel.messageToSend, axis: .vertical)
                        .textFieldStyle(.roundedBorder)
                        .lineLimit(3...6)
                        .disabled(!viewModel.isConnected)
                    
                    Button {
                        viewModel.publishMessage()
                    } label: {
                        Image(systemName: "paperplane.fill")
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(!viewModel.canPublish)
                }
            }
            
            // Quick message buttons
            VStack(alignment: .leading, spacing: 4) {
                Text("Quick Messages:")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        QuickMessageButton(message: "Hello from iOS!", viewModel: viewModel)
                        QuickMessageButton(message: "Test message", viewModel: viewModel)
                        QuickMessageButton(message: "🎉", viewModel: viewModel)
                        QuickMessageButton(message: "{\"status\":\"online\"}", viewModel: viewModel)
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
    }
}

struct QuickMessageButton: View {
    let message: String
    @ObservedObject var viewModel: MQTTViewModel
    
    var body: some View {
        Button {
            viewModel.messageToSend = message
        } label: {
            Text(message)
                .font(.caption2)
                .lineLimit(1)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
        }
        .buttonStyle(.bordered)
        .controlSize(.small)
        .disabled(!viewModel.isConnected)
    }
}

#Preview {
    PublishView(viewModel: MQTTViewModel())
        .padding()
}
