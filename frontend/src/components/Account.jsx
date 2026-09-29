import React, { useState } from 'react';
import { api } from '../lib/api';

export default function Account({ user, onAuthenticate, onSignOut }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const submit = async event => {
    event.preventDefault();
    const details = mode === 'login' ? { email: form.email, password: form.password } : form;
    await onAuthenticate(mode, details);
  };
  if (user) {
    return (
      <section className="panel">
        <p>YOUR ACCOUNT</p>
        <h2>Hi, {user.name}.</h2>
        <p>Orders are tied to {user.email}.</p>
        <button className="primary" onClick={onSignOut}>Sign out</button>
      </section>
    );
  }
  return (
    <section className="panel">
    <p>YOUR ACCOUNT</p>
    <h2>{mode === 'login' ? 'Welcome back.' : 'Create an account.'}</h2>
    <form onSubmit={submit}>
      {mode === 'register' && <label>Name<input required value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} /></label>}
      <label>Email<input required type="email" value={form.email} onChange={event => setForm({ ...form, email: event.target.value })} /></label>
      <label>Password<input required minLength="8" type="password" value={form.password} onChange={event => setForm({ ...form, password: event.target.value })} /></label>
      <button className="primary">{mode === 'login' ? 'Sign in' : 'Create account'}</button>
    </form>
    <a className="text google-login" href={api.googleLoginUrl}>
      Continue with Google
    </a>
    <button className="text" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'Need an account?' : 'Already have an account?'}</button>
    </section>
  );
}
