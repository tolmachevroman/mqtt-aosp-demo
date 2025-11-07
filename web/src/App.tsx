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

  // Connect to MQTT broker on component mount
  useEffect(() => {
    const connectToMQTT = () => {
      setStatus('connecting')
      
      // Connect to the WebSocket MQTT server
      const client = mqtt.connect('ws://localhost:8080')
      clientRef.current = client
      
      client.on('connect', () => {
        console.log('Connected to MQTT broker')
        setStatus('connected')
        
        // Subscribe to demo topics to show incoming messages
        client.subscribe('demo/+', (err) => {
          if (err) {
            console.error('Failed to subscribe:', err)
          } else {
            console.log('Subscribed to demo topics')
          }
        })
      })
      
      client.on('message', (topic, payload) => {
        const messageText = payload.toString()
        console.log(`Received message on ${topic}:`, messageText)
        
        setReceivedMessages(prev => [...prev, {
          topic,
          message: messageText,
          timestamp: new Date()
        }].slice(-10)) // Keep only last 10 messages
      })
      
      client.on('error', (err) => {
        console.error('MQTT connection error:', err)
        setStatus('error')
      })
      
      client.on('close', () => {
        console.log('MQTT connection closed')
        setStatus('disconnected')
      })
    }
    
    connectToMQTT()
    
    // Cleanup on component unmount
    return () => {
      if (clientRef.current) {
        clientRef.current.end()
      }
    }
  }, [])

  const sendMessage = async () => {
    if (!message.trim() || !clientRef.current || status !== 'connected') return
    
    setStatus('sending')
    try {
      // Publish message to MQTT broker
      console.log('Sending message:', { topic, message })
      
      clientRef.current.publish(topic, message, { qos: 0 }, (err) => {
        if (err) {
          console.error('Error publishing message:', err)
          setStatus('error')
          setTimeout(() => setStatus('connected'), 2000)
        } else {
          console.log('Message published successfully')
          setMessage('')
          setStatus('connected') // Directly back to connected without intermediate 'sent' status
        }
      })
    } catch (error) {
      console.error('Error sending message:', error)
      setStatus('error')
      setTimeout(() => setStatus('connected'), 2000)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background to-muted/20 p-6 md:p-8">
      <div className="max-w-3xl mx-auto space-y-8">
        {/* Header */}
        <div className="text-center space-y-6">
          <div className="flex items-center justify-center">
            <h1 className="text-5xl font-bold bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
              MQTT Demo
            </h1>
          </div>
          <p className="text-muted-foreground text-xl max-w-2xl mx-auto leading-relaxed">
            Send real-time messages to connected Android devices and other MQTT clients
          </p>
        </div>

        {/* Connection Status Banner */}
        <div className={`p-6 rounded-2xl border-2 transition-all duration-300 ${
          status === 'connected'
            ? 'bg-emerald-50 border-emerald-200 dark:bg-emerald-950/30 dark:border-emerald-700'
            : 'bg-red-50 border-red-200 dark:bg-red-950/30 dark:border-red-700'
        }`}>
          <div className="flex items-center justify-center">
            <span className={`font-medium text-lg ${
              status === 'connected' ? 'text-emerald-700 dark:text-emerald-300' : 'text-red-700 dark:text-red-300'
            }`}>
              {status === 'connected' ? 'Connected to MQTT server' : 
               status === 'connecting' ? 'Connecting to MQTT server...' :
               status === 'error' ? 'Connection error - check server' :
               'Disconnected from MQTT server'}
            </span>
          </div>
        </div>
        
        {/* Send Message Card */}
        <Card className="shadow-xl border-0 bg-card/50 backdrop-blur-sm">
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
          <CardContent className="space-y-6">
            <div className="space-y-4">
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
            
            <div className="space-y-4">
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
              className={`w-full h-14 font-semibold text-lg transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98] ${
                status === 'connected' 
                  ? 'cursor-pointer hover:shadow-lg' 
                  : 'cursor-not-allowed opacity-60'
              }`}
              variant={
                status === 'error'
                  ? 'destructive'
                  : 'default'
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
          </CardContent>
        </Card>

        {/* Received Messages */}
        {receivedMessages.length > 0 && (
          <Card className="shadow-xl border-0 bg-card/50 backdrop-blur-sm">
            <CardHeader className="pb-6">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-2xl">Recent Messages</CardTitle>
                  <CardDescription className="text-base mt-1">
                    Live messages from connected devices
                  </CardDescription>
                </div>
                <div className="bg-green-500/10 text-green-600 px-4 py-2 rounded-full text-base font-medium">
                  {receivedMessages.length} messages
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4 max-h-80 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-muted scrollbar-track-transparent">
                {receivedMessages.slice().reverse().map((msg, index) => (
                  <div 
                    key={index} 
                    className="group p-5 bg-gradient-to-r from-muted/50 to-muted/30 rounded-xl border transition-all duration-200 hover:shadow-md hover:scale-[1.01] cursor-default"
                  >
                    <div className="flex items-start justify-between space-x-4">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center space-x-3 mb-3">
                          <code className="font-mono text-base font-semibold text-primary bg-primary/10 px-3 py-1.5 rounded-lg">
                            {msg.topic}
                          </code>
                        </div>
                        <p className="text-foreground font-medium break-words leading-relaxed text-base">
                          {msg.message}
                        </p>
                      </div>
                      <div className="flex flex-col items-end space-y-2 flex-shrink-0">
                        <div className="text-sm text-muted-foreground font-medium">
                          {msg.timestamp.toLocaleTimeString()}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
              {receivedMessages.length >= 10 && (
                <div className="mt-6 text-center">
                  <p className="text-sm text-muted-foreground">
                    Showing last 10 messages • Messages auto-refresh
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        )}
        
        {/* Footer Info */}
        <div className="text-center space-y-6">
          <div className="p-6 bg-muted/30 rounded-xl border-dashed border-2 border-muted">
            <div className="flex items-center justify-center space-x-3 mb-4">
              <h3 className="text-xl font-semibold text-foreground">How it works</h3>
            </div>
            <p className="text-muted-foreground text-base leading-relaxed max-w-2xl mx-auto">
              Messages are broadcast to all devices subscribed to the topic. 
              Connect your Android app to <code className="bg-muted px-2 py-1 rounded text-sm">mqtt://localhost:1883</code> 
              to receive messages in real-time.
            </p>
          </div>
          
          <div className="flex items-center justify-center space-x-8 text-sm text-muted-foreground">
            <div className="flex items-center space-x-2">
              <span>WebSocket: ws://localhost:8080</span>
            </div>
            <div className="flex items-center space-x-2">
              <span>TCP: localhost:1883</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default App
