import { useState } from 'react'
import './App.css'
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"

function App() {
  const [message, setMessage] = useState('')
  const [topic, setTopic] = useState('demo/messages')
  const [status, setStatus] = useState<'idle' | 'sending' | 'sent' | 'error'>('idle')

  const sendMessage = async () => {
    if (!message.trim()) return
    
    setStatus('sending')
    try {
      // TODO: Implement MQTT message sending
      console.log('Sending message:', { topic, message })
      
      // Simulate sending for now
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      setStatus('sent')
      setMessage('')
      setTimeout(() => setStatus('idle'), 2000)
    } catch (error) {
      console.error('Error sending message:', error)
      setStatus('error')
      setTimeout(() => setStatus('idle'), 2000)
    }
  }

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-md mx-auto space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-foreground mb-2">MQTT Demo</h1>
          <p className="text-muted-foreground">Send messages to Android devices</p>
        </div>
        
        <Card>
          <CardHeader>
            <CardTitle>Send Message</CardTitle>
            <CardDescription>
              Send MQTT messages to connected devices
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="topic">Topic</Label>
              <Input
                id="topic"
                type="text"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                placeholder="Enter MQTT topic"
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="message">Message</Label>
              <Textarea
                id="message"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                rows={4}
                placeholder="Enter your message"
              />
            </div>
            
            <Button
              onClick={sendMessage}
              disabled={!message.trim() || status === 'sending'}
              className="w-full"
              variant={
                status === 'sent' 
                  ? 'default'
                  : status === 'error'
                  ? 'destructive'
                  : 'default'
              }
            >
              {status === 'sending' && 'Sending...'}
              {status === 'sent' && 'Message Sent!'}
              {status === 'error' && 'Error Sending'}
              {status === 'idle' && 'Send Message'}
            </Button>
          </CardContent>
        </Card>
        
        <div className="text-center text-sm text-muted-foreground">
          <p>Make sure your MQTT server is running on the server.</p>
        </div>
      </div>
    </div>
  )
}

export default App
