# MQTT Integration Guide

Complete guide for the MQTT implementation in the Android demo app.

## Overview

This Android app uses **HiveMQ MQTT Client** to connect to an MQTT broker and exchange messages with
other clients (web, mobile, IoT devices, etc.).

## Quick Start

### 1. Start the MQTT Broker

```bash
cd server
npm start
```

The broker runs on:

- **MQTT TCP**: `localhost:1883`
- **MQTT WebSocket**: `ws://localhost:8080`
- **HTTP API**: `http://localhost:3001`

### 2. Configure & Run Android App

**For Emulator** (default):

```
Broker URL: tcp://10.0.2.2:1883
```

**For Physical Device**:

```
1. Find your computer's IP: ifconfig | grep "inet "
2. Update in app: tcp://YOUR_IP:1883
3. Ensure same WiFi network
```

### 3. Test Communication

1. Launch app → Click "Connect"
2. Subscribe to `test/topic`
3. Send a message
4. Launch web app or another client
5. Messages sync in real-time!

## Architecture

### MQTT Components

```
MqttApplication.kt
    ↓ initializes
MqttManager.kt (Singleton)
    ↓ injected into
MqttViewModel.kt
    ↓ provides state to
MainActivity.kt (Compose UI)
...existing code...
