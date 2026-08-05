import React from 'react';

export default function Header({ cartCount, user, onNavigate }) {
  return (
    <header>
      <button className="brand" onClick={() => onNavigate('shop')}>
        kindred<span>goods</span>
      </button>
    <nav>
      <button onClick={() => onNavigate('shop')}>Shop</button>
        <button onClick={() => onNavigate('account')}>{user ? user.name : 'Account'}</button>
        <button className="cart-button" onClick={() => onNavigate('cart')}>
          Bag <b>{cartCount}</b>
        </button>
    </nav>
    </header>
  );
}
