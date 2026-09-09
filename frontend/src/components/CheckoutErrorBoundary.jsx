import React from 'react';

// Stripe Elements runs outside our application code. If it fails to initialise,
// show a useful message instead of leaving the whole React page blank.
export default class CheckoutErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error) {
    console.error('Unable to render the Stripe payment form.', error);
  }

  componentDidUpdate(previousProps) {
    if (previousProps.resetKey !== this.props.resetKey && this.state.error) {
      this.setState({ error: null });
    }
  }

  render() {
    if (this.state.error) {
      return (
        <section className="panel">
          <p>PAYMENT FORM ERROR</p>
          <h2>Stripe could not load the payment form.</h2>
          <p>{this.state.error.message || 'Check the browser console for details.'}</p>
        </section>
      );
    }

    return this.props.children;
  }
}
