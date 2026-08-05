import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import { useState } from 'react';
import { formatCurrency } from '../lib/formatters';

const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = publishableKey ? loadStripe(publishableKey) : null;

function PaymentForm({ total, onPaymentComplete }) {
  const stripe = useStripe();
  const elements = useElements();
  const [error, setError] = useState('');
  const [isPaying, setIsPaying] = useState(false);

  const submit = async event => {
    event.preventDefault();

    if (!stripe || !elements) return;

    setIsPaying(true);
    setError('');
    const result = await stripe.confirmPayment({ elements, redirect: 'if_required' });

    if (result.error) {
      setError(result.error.message || 'Your payment could not be completed.');
      setIsPaying(false);
      return;
    }

    if (result.paymentIntent?.status !== 'succeeded') {
      setError('Your payment is still being processed. Please try again shortly.');
      setIsPaying(false);
      return;
    }

    await onPaymentComplete(result.paymentIntent.id);
    setIsPaying(false);
  };

  return (
    <form onSubmit={submit}>
      <PaymentElement />
      {error && <p className="payment-error">{error}</p>}
      <button className="primary" disabled={!stripe || isPaying}>
        {isPaying ? 'Processing payment…' : `Pay ${formatCurrency(total)}`}
      </button>
    </form>
  );
}

export default function Checkout({ clientSecret, total, onPaymentComplete }) {
  if (!publishableKey) {
    return (
      <section className="panel">
        <p>PAYMENT SETUP REQUIRED</p>
        <h2>Stripe is not configured.</h2>
        <p>Set VITE_STRIPE_PUBLISHABLE_KEY to your Stripe publishable test key, then restart Vite.</p>
      </section>
    );
  }

  return (
    <section className="panel">
      <p>SECURE CHECKOUT</p>
      <h2>Pay {formatCurrency(total)}</h2>
      <Elements stripe={stripePromise} options={{ clientSecret }}>
        <PaymentForm total={total} onPaymentComplete={onPaymentComplete} />
      </Elements>
    </section>
  );
}
