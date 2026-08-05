const API_URL = 'http://localhost:8080/api';

async function request(path, options) {
  const response = await fetch(`${API_URL}${path}`, options);
  const body = await response.json();
  if (!response.ok) throw new Error(body.message || 'Something went wrong.');
  return body;
}

export const api = {
  getProducts: () => request('/products'),
  login: (credentials) => request('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(credentials) }),
  register: (details) => request('/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(details) }),
  createCheckout: (checkout) => request('/checkout', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(checkout) }),
};
