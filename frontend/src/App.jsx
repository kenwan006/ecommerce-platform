import React, { useEffect, useMemo, useState } from 'react';
import Account from './components/Account';
import Cart from './components/Cart';
import ChatBox from './components/ChatBox';
import Checkout from './components/Checkout';
import CheckoutErrorBoundary from './components/CheckoutErrorBoundary';
import Header from './components/Header';
import Notice from './components/Notice';
import Shop from './components/Shop';
import Tracking from './components/Tracking';
import Warehouse from './components/Warehouse';
import { api } from './lib/api';

const readStorage = (key, fallback) => JSON.parse(localStorage.getItem(key) || fallback);

export default function App() {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState([]);
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
  useEffect(() => {
    if (!user) {
      setCart([]);
      return;
    }
    api.getCart()
      .then(response => setCart(response.items))
      .catch(() => setNotice('Could not load your saved bag.'));
  }, [user]);
  useEffect(() => {
    if (user) localStorage.setItem('user', JSON.stringify(user));
    else localStorage.removeItem('user');
  }, [user]);
  const cartCount = cart.reduce((sum, item) => sum + item.quantity, 0);
  const total = useMemo(
    () => cart.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0),
    [cart],
  );
  const addToCart = async product => {
    if (!user) {
      setNotice('Sign in to save items to your bag.');
      setView('account');
      return;
    }
    try {
      const savedCart = await api.addCartItem(product.id);
      setCart(savedCart.items);
      setNotice(`${product.name} added to cart.`);
    } catch (error) {
      setNotice(error.message);
    }
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
    try {
      await api.clearCart();
    } catch (error) {
      setNotice(`Payment received, but your bag could not be cleared: ${error.message}`);
    }
    setCart([]);
    setPaymentIntent(null);
    setCheckoutId(null);
    setNotice('Payment received. Your order is being confirmed.');
    setView('shop');
  };
  const authenticate = async (mode, details) => {
    try {
      const loggedInUser = mode === 'login' ? await api.login(details) : await api.register(details);
      localStorage.setItem('accessToken', loggedInUser.accessToken);
      setUser(loggedInUser);
      setNotice(`Welcome, ${loggedInUser.name}.`);
    } catch (error) {
      setNotice(error.message);
    }
  };
  const signOut = () => {
    localStorage.removeItem('accessToken');
    setUser(null);
    setCart([]);
    setPaymentIntent(null);
    setCheckoutId(null);
    setView('shop');
    setNotice('You have signed out.');
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
          onChangeQuantity={async (item, quantity) => {
            try {
              const savedCart = quantity === 0
                ? await api.removeCartItem(item.id)
                : await api.updateCartItem(item.id, quantity);
              setCart(savedCart.items);
            } catch (error) {
              setNotice(error.message);
            }
          }}
          onRemove={async id => {
            try {
              const savedCart = await api.removeCartItem(id);
              setCart(savedCart.items);
            } catch (error) {
              setNotice(error.message);
            }
          }}
          onCheckout={startCheckout}
        />
      )}
      {view === 'checkout' && paymentIntent && (
        <CheckoutErrorBoundary resetKey={paymentIntent.paymentIntentId}>
          <Checkout
            clientSecret={paymentIntent.clientSecret}
            total={paymentIntent.total}
            onPaymentComplete={completeCheckout}
          />
        </CheckoutErrorBoundary>
      )}
      {view === 'account' && (
        <Account
          user={user}
          onAuthenticate={authenticate}
          onSignOut={signOut}
        />
      )}
      {view === 'tracking' && user && <Tracking />}
      {view === 'warehouse' && <Warehouse />}
      <ChatBox />
    </main>
  );
}
