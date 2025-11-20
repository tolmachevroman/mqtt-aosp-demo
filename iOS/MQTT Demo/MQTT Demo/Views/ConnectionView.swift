//
//  ConnectionView.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import SwiftUI

/// View for MQTT broker connection configuration
struct ConnectionView: View {
    @ObservedObject var viewModel: MQTTViewModel
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("MQTT Broker Connection")
                .font(.headline)
            
            // Broker Host
            VStack(alignment: .leading, spacing: 4) {
                Text("Broker Host")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                TextField("localhost", text: $viewModel.brokerHost)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                    .disabled(viewModel.isConnected)
            }
            
            // Broker Port
            VStack(alignment: .leading, spacing: 4) {
                Text("Port")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                TextField("1883", text: $viewModel.brokerPort)
                    .textFieldStyle(.roundedBorder)
                    .keyboardType(.numberPad)
                    .disabled(viewModel.isConnected)
            }
            
            // Connect/Disconnect Button
            Button {
                if viewModel.isConnected {
                    viewModel.disconnect()
                } else {
                    viewModel.connect()
                }
            } label: {
                HStack {
                    Image(systemName: viewModel.isConnected ? "network.slash" : "network")
                    Text(viewModel.isConnected ? "Disconnect" : "Connect")
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(viewModel.isConnected ? .red : .blue)
            .disabled(!viewModel.canConnect && !viewModel.isConnected)
            
            // Connection Status
            HStack {
                Circle()
                    .fill(statusColor)
                    .frame(width: 10, height: 10)
                
                Text(viewModel.connectionState.displayText)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.1), radius: 5, x: 0, y: 2)
    }
    
    private var statusColor: Color {
        switch viewModel.connectionState {
        case .connected:
            return .green
        case .connecting, .disconnecting:
            return .yellow
        case .disconnected:
            return .gray
        case .error:
            return .red
        }
    }
}

#Preview {
    ConnectionView(viewModel: MQTTViewModel())
        .padding()
}
