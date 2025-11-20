//
//  MessagesView.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import SwiftUI

/// View displaying the list of sent and received MQTT messages
struct MessagesView: View {
    @ObservedObject var viewModel: MQTTViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Text("Messages")
                    .font(.headline)
                
                Spacer()
                
                if !viewModel.messages.isEmpty {
                    Button {
                        viewModel.clearMessages()
                    } label: {
                        Label("Clear", systemImage: "trash")
                            .font(.caption)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
            }
            
            // Messages List
            if viewModel.messages.isEmpty {
                EmptyMessagesView()
            } else {
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            ForEach(viewModel.messages) { message in
                                MessageRow(message: message)
                                    .id(message.id)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .frame(maxHeight: 400)
                    .onChange(of: viewModel.messages.count) { _, _ in
                        // Auto-scroll to bottom when new message arrives
                        if let lastMessage = viewModel.messages.last {
                            withAnimation {
                                proxy.scrollTo(lastMessage.id, anchor: .bottom)
                            }
                        }
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

struct MessageRow: View {
    let message: MQTTMessage
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Direction indicator
            Image(systemName: message.direction == .sent ? "arrow.up.circle.fill" : "arrow.down.circle.fill")
                .foregroundColor(message.direction == .sent ? .blue : .green)
                .font(.title3)
            
            // Message content
            VStack(alignment: .leading, spacing: 4) {
                // Topic
                Text(message.topic)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                // Payload
                Text(message.payload)
                    .font(.body)
                    .foregroundColor(.primary)
                
                // Timestamp
                Text(message.formattedTime)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(.secondarySystemBackground))
        )
    }
}

struct EmptyMessagesView: View {
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "tray")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            
            Text("No messages yet")
                .font(.headline)
                .foregroundColor(.secondary)
            
            Text("Subscribe to a topic and start sending messages!")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
}

#Preview {
    MessagesView(viewModel: MQTTViewModel())
        .padding()
}
