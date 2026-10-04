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
  const [isRefunding, setIsRefunding] = useState(false);

  const confirmRefund = async orderId => {
    if (isRefunding) return;
    setIsRefunding(true);
    try {
      const refund = await api.refundOrder(orderId);
      setMessages(current => [...current, {
        role: 'assistant',
        text: `Your refund of ${refund.amount} ${refund.currency} for order #${refund.orderId} has been processed.`,
      }]);
    } catch (error) {
      setMessages(current => [...current, {
        role: 'assistant',
        text: `The refund could not be processed: ${error.message}`,
      }]);
    } finally {
      setIsRefunding(false);
    }
  };

  const sendMessage = async event => {
    event.preventDefault();
    const text = message.trim();
    if (!text || isSending) return;

    setMessage('');
    setMessages(current => [...current, { role: 'user', text }]);
    setIsSending(true);
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 35_000);
    try {
      const response = await api.askAi(text, controller.signal);
      setMessages(current => [...current, {
        role: 'assistant',
        text: response.answer || response.reply || response.message || 'I could not generate a response.',
        refundAction: response.refund_action,
      }]);
    } catch (error) {
      const message = error.name === 'AbortError'
        ? 'The assistant took too long to respond. Please try again.'
        : `Sorry, I could not reach the assistant. ${error.message}`;
      setMessages(current => [...current, {
        role: 'assistant',
        text: message,
      }]);
    } finally {
      window.clearTimeout(timeout);
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
              <div className={`chatbox-message ${entry.role}`} key={`${entry.role}-${index}`}>
                <p>{entry.text}</p>
                {entry.refundAction && (
                  <button
                    className="chatbox-refund-button"
                    type="button"
                    disabled={isRefunding}
                    onClick={() => confirmRefund(entry.refundAction.order_id)}
                  >
                    {isRefunding ? 'Processing refund…' : `Confirm refund for order #${entry.refundAction.order_id}`}
                  </button>
                )}
              </div>
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
