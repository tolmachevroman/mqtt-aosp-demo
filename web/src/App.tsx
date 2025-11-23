import { useState, useEffect, useRef } from 'react'
import './App.css'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import mqtt from 'mqtt'

function App() {
  const [message, setMessage] = useState('')
  const [topic, setTopic] = useState('demo/messages')
  const [status, setStatus] = useState<'disconnected' | 'connecting' | 'connected' | 'sending' | 'sent' | 'error'>('disconnected')
  const [receivedMessages, setReceivedMessages] = useState<Array<{topic: string, message: string, timestamp: Date}>>([])
  const clientRef = useRef<mqtt.MqttClient | null>(null)
  const connectionAttemptRef = useRef<boolean>(false)

  // Connect to MQTT broker on component mount
  useEffect(() => {
    // Prevent multiple simultaneous connection attempts
    if (connectionAttemptRef.current) {
      console.log('Connection attempt already in progress, skipping...')
      return
    }

    const connectToMQTT = () => {
      connectionAttemptRef.current = true
      setStatus('connecting')
      
      // Generate a unique client ID
      const clientId = 'mqtt-demo-web-' + Math.random().toString(16).substr(2, 8)
      console.log('Attempting to connect with client ID:', clientId)
      
      // Connect to the public Mosquitto test broker (WebSocket)
      const client = mqtt.connect('wss://test.mosquitto.org:8081', {
        reconnectPeriod: 3000, // Reconnect after 3 seconds (increased)
        connectTimeout: 10 * 1000, // 10 seconds (reduced)
        keepalive: 30, // Send ping every 30 seconds (reduced)
        clean: true, // Clean session
        reschedulePings: true,
        clientId: clientId,
      })
      clientRef.current = client
      
      client.on('connect', () => {
        console.log('✅ Connected to MQTT broker with client ID:', clientId)
        connectionAttemptRef.current = false
        setStatus('connected')
        
        // Subscribe to demo topics to show incoming messages
        client.subscribe('demo/+', (err) => {
          if (err) {
            console.error('❌ Failed to subscribe:', err)
          } else {
            console.log('📧 Subscribed to demo topics')
          }
        })
      })
      
      client.on('reconnect', () => {
        console.log('🔄 Attempting to reconnect to MQTT broker...')
        setStatus('connecting')
      })
      
      client.on('message', (topic, payload) => {
        const messageText = payload.toString()
        console.log(`📨 Received message on ${topic}:`, messageText)
        
        setReceivedMessages(prev => [...prev, {
          topic,
          message: messageText,
          timestamp: new Date()
        }].slice(-10)) // Keep only last 10 messages
      })
      
      client.on('error', (err) => {
        console.error('❌ MQTT connection error:', err)
        connectionAttemptRef.current = false
        setStatus('error')
      })
      
      client.on('close', () => {
        console.log('🔌 MQTT connection closed')
        connectionAttemptRef.current = false
        setStatus('disconnected')
      })
      
      client.on('disconnect', () => {
        console.log('🔌 MQTT client disconnected')
        connectionAttemptRef.current = false
        setStatus('disconnected')
      })
      
      client.on('offline', () => {
        console.log('📱 MQTT client went offline')
        connectionAttemptRef.current = false
        setStatus('disconnected')
      })
    }
    
    connectToMQTT()
    
    // Cleanup on component unmount
    return () => {
      connectionAttemptRef.current = false
      if (clientRef.current) {
        console.log('🧹 Cleaning up MQTT connection...')
        clientRef.current.removeAllListeners() // Remove all event listeners
        clientRef.current.end(true) // Force close
        clientRef.current = null
      }
    }
  }, [])

  const sendMessage = async () => {
    if (!message.trim() || !clientRef.current || status !== 'connected' || connectionAttemptRef.current) {
      console.log('❌ Cannot send message:', { 
        hasMessage: !!message.trim(), 
        hasClient: !!clientRef.current, 
        status, 
        connectionInProgress: connectionAttemptRef.current 
      })
      return
    }
    
    setStatus('sending')
    try {
      // Publish message to MQTT broker
      console.log('📤 Sending message:', { topic, message })
      
      clientRef.current.publish(topic, message, { qos: 0 }, (err) => {
        if (err) {
          console.error('❌ Error publishing message:', err)
          setStatus('error')
          setTimeout(() => setStatus('connected'), 2000)
        } else {
          console.log('✅ Message published successfully')
          setMessage('')
          setStatus('connected') // Directly back to connected without intermediate 'sent' status
        }
      })
    } catch (error) {
      console.error('❌ Error sending message:', error)
      setStatus('error')
      setTimeout(() => setStatus('connected'), 2000)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background to-muted/20 p-6 md:p-8 flex items-center justify-center">
      <div className="w-full max-w-6xl space-y-6">
        {/* Header */}
        <div className="text-center space-y-4">
          <div className="flex items-center justify-center">
            <h1 className="text-5xl font-bold bg-gradient-to-r from-slate-900 to-slate-600 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
              MQTT Demo
            </h1>
          </div>
          <p className="text-muted-foreground text-xl max-w-2xl mx-auto leading-relaxed">
            Send real-time messages to connected Android devices and other MQTT clients
          </p>
        </div>

        {/* Connection Status - Outside Grid */}
        <div className="flex justify-end mb-3">
          <div className={`inline-flex items-center px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-300 ${
            status === 'connected'
              ? 'bg-emerald-100 text-emerald-700 border border-emerald-200 dark:bg-emerald-950/50 dark:text-emerald-300 dark:border-emerald-700'
              : status === 'connecting'
              ? 'bg-blue-100 text-blue-700 border border-blue-200 dark:bg-blue-950/50 dark:text-blue-300 dark:border-blue-700'
              : 'bg-red-100 text-red-700 border border-red-200 dark:bg-red-950/50 dark:text-red-300 dark:border-red-700'
          }`}>
            <div className={`w-2 h-2 rounded-full mr-2 ${
              status === 'connected' ? 'bg-emerald-500 animate-pulse' : 
              status === 'connecting' ? 'bg-blue-500 animate-pulse' :
              'bg-red-500'
            }`} />
            {status === 'connected' ? 'Connected' : 
             status === 'connecting' ? 'Connecting...' :
             status === 'error' ? 'Error' :
             'Disconnected'}
          </div>
        </div>

        {/* Main Content - Two Column Layout */}
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-12 h-[70vh]">
          {/* Send Message Card */}
          <Card className="shadow-xl bg-card/50 backdrop-blur-sm flex flex-col h-full">
            <CardHeader className="pb-6">
              <div className="flex items-center">
                <div>
                  <CardTitle className="text-2xl">Send Message</CardTitle>
                  <CardDescription className="text-base mt-1">
                    Publish MQTT messages to connected devices
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-6 flex-1 flex flex-col">
            <div className="space-y-3">
              <Label htmlFor="topic" className="text-base font-semibold">
                Topic
              </Label>
              <Input
                id="topic"
                type="text"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                placeholder="e.g., demo/messages, sensors/temperature"
                className="transition-all duration-200 focus:ring-2 focus:ring-primary/20 hover:border-primary/50 text-base h-12"
              />
              <p className="text-sm text-muted-foreground">
                Topic path for message routing (use / for hierarchy)
              </p>
            </div>
            
            <div className="space-y-3">
              <Label htmlFor="message" className="text-base font-semibold">
                Message
              </Label>
              <Textarea
                id="message"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                rows={4}
                placeholder="Enter your message here... (JSON, text, or any format)"
                className="transition-all duration-200 focus:ring-2 focus:ring-primary/20 hover:border-primary/50 resize-none text-base"
              />
              <div className="flex justify-between items-center">
                <p className="text-sm text-muted-foreground">
                  Message content (supports any text format)
                </p>
                <span className={`text-sm ${message.length > 100 ? 'text-orange-500' : 'text-muted-foreground'}`}>
                  {message.length}/1000
                </span>
              </div>
            </div>
            
            <Button
              onClick={sendMessage}
              disabled={!message.trim() || status !== 'connected'}
              className={`w-full h-12 font-semibold text-base transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] bg-slate-800 hover:bg-slate-700 text-white border-slate-700 ${
                status === 'connected' 
                  ? 'cursor-pointer hover:shadow-lg' 
                  : 'cursor-not-allowed opacity-60'
              }`}
              variant={
                status === 'error'
                  ? 'destructive'
                  : 'secondary'
              }
            >
              <span>
                {status === 'disconnected' && 'Disconnected'}
                {status === 'connecting' && 'Connecting...'}
                {status === 'connected' && 'Send Message'}
                {status === 'sending' && 'Sending...'}
                {status === 'error' && 'Connection Error'}
              </span>
            </Button>
            
            {/* How it works section - pushed to bottom */}
            <div className="mt-auto pt-6 border-t border-muted">
              <h4 className="text-lg font-semibold text-foreground mb-3">How it works</h4>
              <p className="text-muted-foreground text-sm leading-relaxed mb-4">
                Messages are broadcast to all devices subscribed to the topic. 
                Connect your Android app to <code className="font-mono bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 px-2 py-1 rounded text-xs border mx-1">mqtt://localhost:1883</code> to receive messages in real-time.
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div className="flex items-center space-x-2">
                  <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                  <span className="text-muted-foreground">WebSocket: </span>
                  <code className="font-mono bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 px-1.5 py-0.5 rounded border text-xs">ws://localhost:8080</code>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  <span className="text-muted-foreground">TCP: </span>
                  <code className="font-mono bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 px-1.5 py-0.5 rounded border text-xs">localhost:1883</code>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

          {/* Received Messages */}
          <Card className="shadow-xl bg-card/50 backdrop-blur-sm flex flex-col h-full">
          <CardHeader className="pb-6">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-2xl">Recent Messages</CardTitle>
                <CardDescription className="text-base mt-1">
                  Live messages from connected devices
                </CardDescription>
              </div>
              {receivedMessages.length > 0 && (
                <div className="bg-green-500/10 text-green-600 px-4 py-2 rounded-full text-sm font-medium">
                  {receivedMessages.length} messages
                </div>
              )}
            </div>
          </CardHeader>
          <CardContent className="flex-1 flex flex-col">
            {receivedMessages.length > 0 ? (
              <>
                <div className="space-y-4 flex-1 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-muted scrollbar-track-transparent">
                  {receivedMessages.slice().reverse().map((msg, index) => (
                    <div 
                      key={index} 
                      className="group p-5 bg-gradient-to-r from-muted/50 to-muted/30 rounded-xl border"
                    >
                      <div className="flex items-start justify-between space-x-4">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center space-x-3 mb-3">
                            <code className="font-mono text-base font-semibold text-primary bg-primary/10 px-3 py-2 rounded">
                              {msg.topic}
                            </code>
                          </div>
                          <p className="text-foreground font-medium break-words leading-relaxed text-base">
                            {msg.message}
                          </p>
                        </div>
                        <div className="flex flex-col items-end flex-shrink-0">
                          <div className="text-base text-muted-foreground font-medium">
                            {msg.timestamp.toLocaleTimeString()}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                {receivedMessages.length >= 10 && (
                  <div className="mt-4 text-center">
                    <p className="text-sm text-muted-foreground">
                      Showing last 10 messages • Messages auto-refresh
                    </p>
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-8 flex-1 flex items-center justify-center">
                <p className="text-muted-foreground text-sm">
                  No messages received yet. Send a message to see it appear here.
                </p>
              </div>
            )}
          </CardContent>
        </Card>
        </div>
      </div>
    </div>
  )
}

export default App
