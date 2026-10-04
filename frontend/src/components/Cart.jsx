import React from 'react';
import { formatCurrency } from '../lib/formatters';

export default function Cart({ cart, total, user, onChangeQuantity, onRemove, onCheckout }) {
  return (
    <section className="panel">
    <p>YOUR BAG</p>
    <h2>{cart.length ? 'Ready when you are.' : 'Your bag is empty.'}</h2>
      {cart.map(item => (
        <div className="line" key={item.id}>
          <span>{item.name}</span>
          <span className="quantity-controls">
            <button aria-label={`Decrease ${item.name} quantity`} onClick={() => onChangeQuantity(item, item.quantity - 1)}>−</button>
            {item.quantity}
            <button aria-label={`Increase ${item.name} quantity`} onClick={() => onChangeQuantity(item, item.quantity + 1)}>+</button>
          </span>
          <span>{formatCurrency(item.price * item.quantity)}</span>
          <button onClick={() => onRemove(item.id)}>Remove</button>
        </div>
      ))}
      {cart.length > 0 && (
        <>
          <div className="total"><span>Total</span><strong>{formatCurrency(total)}</strong></div>
          <button className="primary" onClick={onCheckout}>
            {user ? 'Continue to secure payment' : 'Sign in to checkout'}
          </button>
        </>
      )}
    </section>
  );
}
