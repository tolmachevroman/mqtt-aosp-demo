//
//  ContentView.swift
//  MQTT Demo
//
//  Created by Roman Tolmachev on 20-11-25.
//

import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = MQTTViewModel()
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Connection Section
                    ConnectionView(viewModel: viewModel)
                    
                    // Subscription Section
                    SubscriptionView(viewModel: viewModel)
                    
                    // Publish Section
                    PublishView(viewModel: viewModel)
                    
                    // Messages Section
                    MessagesView(viewModel: viewModel)
                    
                    // Status Message
                    StatusBanner(message: viewModel.statusMessage)
                }
                .padding()
            }
            .navigationTitle("MQTT Demo")
            .navigationBarTitleDisplayMode(.large)
            .alert("Error", isPresented: $viewModel.showError) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(viewModel.errorMessage)
            }
        }
    }
}

struct StatusBanner: View {
    let message: String
    
    var body: some View {
        HStack {
            Image(systemName: "info.circle")
                .foregroundColor(.blue)
            
            Text(message)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Spacer()
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(8)
    }
}

#Preview {
    ContentView()
}
