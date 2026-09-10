const API_URL = 'http://localhost:8080/api';
const AI_API_URL = 'http://localhost:8000/api';

async function request(path, options, apiUrl = API_URL) {
  const accessToken = localStorage.getItem('accessToken');
  const response = await fetch(`${apiUrl}${path}`, {
    ...options,
    headers: {
      ...(options?.headers || {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(body?.detail || body?.message || 'Something went wrong.');
  return body;
}

export const api = {
  getProducts: () => request('/products'),
  login: (credentials) => request('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(credentials) }),
  register: (details) => request('/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(details) }),
  currentUser: () => request('/auth/me'),
  googleLoginUrl: 'http://localhost:8080/oauth2/authorization/google',
  askAi: (message, signal) => request('/ai/ask', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
    signal,
  }, AI_API_URL),
  createCheckout: (checkout) => request('/checkout', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(checkout) }),
};
