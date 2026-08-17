import React, { useState } from 'react';
import { api } from '../lib/api';

const welcomeMessage = {
  role: 'assistant',
  text: 'Hi! Ask me anything about the shop or its products.',
};

export default function ChatBox() {
  const [isOpen, setIsOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [messages, setMessages] = useState([welcomeMessage]);
  const [isSending, setIsSending] = useState(false);

  const sendMessage = async event => {
    event.preventDefault();
    const text = message.trim();
    if (!text || isSending) return;

    setMessage('');
    setMessages(current => [...current, { role: 'user', text }]);
    setIsSending(true);
    try {
      const response = await api.askAi(text);
      setMessages(current => [...current, {
        role: 'assistant',
        text: response.answer || response.reply || response.message || 'I could not generate a response.',
      }]);
    } catch (error) {
      setMessages(current => [...current, {
        role: 'assistant',
        text: `Sorry, I could not reach the assistant. ${error.message}`,
      }]);
    } finally {
      setIsSending(false);
    }
  };

  return (
    <aside className={`chatbox ${isOpen ? 'chatbox-open' : ''}`} aria-label="Shopping assistant">
      {isOpen && (
        <section className="chatbox-panel">
          <header className="chatbox-header">
            <div>
              <span>SHOP ASSISTANT</span>
              <strong>How can I help?</strong>
            </div>
            <button type="button" onClick={() => setIsOpen(false)} aria-label="Close chat">×</button>
          </header>
          <div className="chatbox-messages" aria-live="polite">
            {messages.map((entry, index) => (
              <p className={`chatbox-message ${entry.role}`} key={`${entry.role}-${index}`}>{entry.text}</p>
            ))}
            {isSending && <p className="chatbox-message assistant">Thinking…</p>}
          </div>
          <form className="chatbox-form" onSubmit={sendMessage}>
            <label className="sr-only" htmlFor="chat-message">Message</label>
            <input
              id="chat-message"
              value={message}
              onChange={event => setMessage(event.target.value)}
              placeholder="Ask a question…"
              disabled={isSending}
            />
            <button type="submit" disabled={!message.trim() || isSending}>Send</button>
          </form>
        </section>
      )}
      <button className="chatbox-toggle" type="button" onClick={() => setIsOpen(open => !open)}>
        {isOpen ? 'Close assistant' : 'Chat with us'}
      </button>
    </aside>
  );
}
