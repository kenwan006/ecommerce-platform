import React, { useEffect, useState } from 'react';
import { api } from '../lib/api';

export default function Tracking() {
  const [orders, setOrders] = useState([]);
  const [shipment, setShipment] = useState(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getOrders()
      .then(setOrders)
      .catch(error => setMessage(error.message))
      .finally(() => setLoading(false));
  }, []);

  const track = async orderId => {
    setMessage('');
    setShipment(null);
    try {
      setShipment(await api.getOrderShipment(orderId));
    } catch (error) {
      setMessage('This order has not been shipped yet.');
    }
  };

  return (
    <section className="tracking-page">
      <p>ORDER TRACKING</p>
      <h1>Track your shipment</h1>
      <span>Choose one of your orders to view its carrier updates.</span>
      {message && <p className="warehouse-error">{message}</p>}
      {loading && <p>Loading your orders…</p>}
      {!loading && <div className="tracking-layout">
        <article className="tracking-card">
          <h2>Your orders</h2>
          {orders.map(order => (
            <button className="tracking-order" key={order.id} onClick={() => track(order.id)}>
              <span><strong>Order #{order.id}</strong>{order.items.map(item => item.productName).join(', ')}</span>
              <b>{order.status}</b>
            </button>
          ))}
          {!orders.length && <p className="warehouse-empty">You have no orders yet.</p>}
        </article>
        <article className="tracking-card">
          <h2>Shipment updates</h2>
          {!shipment && <p className="warehouse-empty">Select a shipped order to view its timeline.</p>}
          {shipment && <>
            <div className="tracking-summary">
              <strong>{shipment.status.replaceAll('_', ' ')}</strong>
              <span>{shipment.carrier} · {shipment.trackingNumber}</span>
            </div>
            {shipment.events.map(event => (
              <div className="tracking-event" key={event.id}>
                <b>{event.status.replaceAll('_', ' ')}</b>
                <span>{event.location || 'Location unavailable'}</span>
                <small>{event.description || 'Carrier update'} · {new Date(event.occurredAt).toLocaleString()}</small>
              </div>
            ))}
            {!shipment.events.length && <p className="warehouse-empty">Label created. Carrier scans will appear here.</p>}
          </>}
        </article>
      </div>}
    </section>
  );
}
