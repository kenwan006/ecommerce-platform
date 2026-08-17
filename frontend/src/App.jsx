import React, { useEffect, useMemo, useState } from 'react';
import Account from './components/Account';
import Cart from './components/Cart';
import ChatBox from './components/ChatBox';
import Checkout from './components/Checkout';
import Header from './components/Header';
import Notice from './components/Notice';
import Shop from './components/Shop';
import { api } from './lib/api';

const readStorage = (key, fallback) => JSON.parse(localStorage.getItem(key) || fallback);

export default function App() {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState(() => readStorage('cart', '[]'));
  const [user, setUser] = useState(() => readStorage('user', 'null'));
  const [view, setView] = useState('shop');
  const [notice, setNotice] = useState('');
  const [paymentIntent, setPaymentIntent] = useState(null);
  const [checkoutId, setCheckoutId] = useState(null);
  useEffect(() => {
    api
      .getProducts()
      .then(setProducts)
      .catch(() => setNotice('Could not reach the API. Start the backend first.'));
  }, []);
  useEffect(() => {
    const accessToken = new URLSearchParams(window.location.hash.slice(1)).get('access_token');
    if (!accessToken) return;
    localStorage.setItem('accessToken', accessToken);
    api.currentUser()
      .then(loggedInUser => {
        setUser(loggedInUser);
        setNotice(`Welcome, ${loggedInUser.name}.`);
      })
      .catch(() => setNotice('Google sign-in could not be completed.'))
      .finally(() => window.history.replaceState({}, '', window.location.pathname));
  }, []);
  useEffect(() => localStorage.setItem('cart', JSON.stringify(cart)), [cart]);
  useEffect(() => {
    if (user) localStorage.setItem('user', JSON.stringify(user));
    else localStorage.removeItem('user');
  }, [user]);
  const cartCount = cart.reduce((sum, item) => sum + item.quantity, 0);
  const total = useMemo(
    () => cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0),
    [cart],
  );
  const addToCart = product => {
    setCart(items => {
      const exists = items.some(item => item.id === product.id);
      if (!exists) return [...items, { ...product, quantity: 1 }];
      return items.map(item =>
        item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item,
      );
    });
    setNotice(`${product.name} added to cart.`);
  };
  const startCheckout = async () => {
    if (!user) {
      setView('account');
      return;
    }
    try {
      const currentCheckoutId = checkoutId || crypto.randomUUID();
      setCheckoutId(currentCheckoutId);
      const intent = await api.createCheckout({
        userId: user.userId,
        checkoutId: currentCheckoutId,
        items: cart.map(item => ({ productId: item.id, quantity: item.quantity })),
      });
      setPaymentIntent(intent);
      setView('checkout');
    } catch (error) {
      setNotice(error.message);
    }
  };

  const completeCheckout = async () => {
    setCart([]);
    setPaymentIntent(null);
    setCheckoutId(null);
    setNotice('Payment received. Your order is being confirmed.');
    setView('shop');
  };
  const authenticate = async (mode, details) => {
    try {
      const loggedInUser = mode === 'login' ? await api.login(details) : await api.register(details);
      setUser(loggedInUser);
      setNotice(`Welcome, ${loggedInUser.name}.`);
    } catch (error) {
      setNotice(error.message);
    }
  };
  return (
    <main>
    <Header cartCount={cartCount} user={user} onNavigate={setView} />
    <Notice message={notice} onDismiss={() => setNotice('')} />
    {view === 'shop' && <Shop products={products} onAddToCart={addToCart} />}
      {view === 'cart' && (
        <Cart
          cart={cart}
          total={total}
          user={user}
          onRemove={id => setCart(items => items.filter(item => item.id !== id))}
          onCheckout={startCheckout}
        />
      )}
      {view === 'checkout' && paymentIntent && (
        <Checkout
          clientSecret={paymentIntent.clientSecret}
          total={paymentIntent.amount / 100}
          onPaymentComplete={completeCheckout}
        />
      )}
      {view === 'account' && (
        <Account
          user={user}
          onAuthenticate={authenticate}
          onSignOut={() => {
            localStorage.removeItem('accessToken');
            setUser(null);
          }}
        />
      )}
      <ChatBox />
    </main>
  );
}
