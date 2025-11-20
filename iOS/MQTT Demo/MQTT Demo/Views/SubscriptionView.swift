//
//  SubscriptionView.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import SwiftUI

/// View for subscribing to MQTT topics
struct SubscriptionView: View {
    @ObservedObject var viewModel: MQTTViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Subscribe to Topic")
                .font(.headline)
            
            // Topic Input
            VStack(alignment: .leading, spacing: 4) {
                Text("Topic Filter")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack {
                    TextField("demo/messages", text: $viewModel.subscribeTopic)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.none)
                        .disabled(!viewModel.isConnected)
                    
                    Button {
                        viewModel.subscribe()
                    } label: {
                        Image(systemName: "arrow.down.circle.fill")
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(!viewModel.canSubscribe)
                }
            }
            
            // Info text
            Text("Supports wildcards: + (single level), # (multi level)")
                .font(.caption2)
                .foregroundColor(.secondary)
            
            // Example topics
            VStack(alignment: .leading, spacing: 4) {
                Text("Examples:")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ExampleTopicChip(topic: "demo/messages", viewModel: viewModel)
                        ExampleTopicChip(topic: "sensors/+/temp", viewModel: viewModel)
                        ExampleTopicChip(topic: "home/#", viewModel: viewModel)
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

struct ExampleTopicChip: View {
    let topic: String
    @ObservedObject var viewModel: MQTTViewModel
    
    var body: some View {
        Button {
            viewModel.subscribeTopic = topic
        } label: {
            Text(topic)
                .font(.caption2)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
        }
        .buttonStyle(.bordered)
        .controlSize(.small)
        .disabled(!viewModel.isConnected)
    }
}

#Preview {
    SubscriptionView(viewModel: MQTTViewModel())
        .padding()
}
