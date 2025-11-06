const aedes = require('aedes')()
const server = require('net').createServer(aedes.handle)
const ws = require('ws')
const express = require('express')
const cors = require('cors')

// MQTT broker configuration
const MQTT_PORT = 1883
const WS_PORT = 8080
const HTTP_PORT = 3001

// Create HTTP server for REST API
const app = express()
app.use(cors())
app.use(express.json())

// MQTT broker event handlers
aedes.on('subscribe', function (subscriptions, client) {
  console.log(`Client ${client.id} subscribed to topics:`, subscriptions.map(s => s.topic))
})

aedes.on('unsubscribe', function (subscriptions, client) {
  console.log(`Client ${client.id} unsubscribed from topics:`, subscriptions)
})

aedes.on('client', function (client) {
  console.log(`Client ${client.id} connected to broker`)
})

aedes.on('clientDisconnect', function (client) {
  console.log(`Client ${client.id} disconnected from broker`)
})

aedes.on('publish', function (packet, client) {
  if (client) {
    console.log(`Message published by ${client.id} to topic ${packet.topic}:`, packet.payload.toString())
  }
})

// Start TCP MQTT server (for Android/native clients)
server.listen(MQTT_PORT, function () {
  console.log(`MQTT broker started on port ${MQTT_PORT}`)
})

// Start WebSocket MQTT server (for web clients)
const wsServer = new ws.Server({ port: WS_PORT })
wsServer.on('connection', function (stream) {
  console.log('WebSocket client connected')
  aedes.handle(stream)
})

console.log(`MQTT WebSocket server started on port ${WS_PORT}`)

// REST API endpoints
app.get('/status', (req, res) => {
  res.json({ 
    status: 'running',
    connections: aedes.connectedClients,
    timestamp: new Date().toISOString()
  })
})

// Endpoint to publish messages via HTTP (for testing)
app.post('/publish', (req, res) => {
  const { topic, message } = req.body
  
  if (!topic || !message) {
    return res.status(400).json({ error: 'Topic and message are required' })
  }
  
  aedes.publish({
    topic: topic,
    payload: Buffer.from(message),
    qos: 0,
    retain: false
  }, (err) => {
    if (err) {
      console.error('Error publishing message:', err)
      return res.status(500).json({ error: 'Failed to publish message' })
    }
    
    console.log(`Message published via HTTP to topic ${topic}: ${message}`)
    res.json({ success: true, topic, message })
  })
})

// Start HTTP server
app.listen(HTTP_PORT, () => {
  console.log(`HTTP API server started on port ${HTTP_PORT}`)
})

console.log('\n🚀 MQTT Demo Server Started!')
console.log(`📡 MQTT TCP: localhost:${MQTT_PORT}`)
console.log(`🌐 MQTT WebSocket: ws://localhost:${WS_PORT}`)
console.log(`🔗 HTTP API: http://localhost:${HTTP_PORT}`)
console.log('\nReady to receive messages! 📨\n')