import { getToken } from '../auth/tokenUtils';

const API_BASE = '';

async function apiRequest(path, options = {}) {
  const token = getToken();

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(API_BASE + path, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let error;
    try {
      const body = await response.json();
      error = new Error(body.message || 'Request failed');
      error.status = response.status;
      error.code = body.code;
    } catch {
      error = new Error('Request failed');
      error.status = response.status;
    }
    throw error;
  }

  if (response.status === 204) return null;
  return response.json();
}

const api = {
  get: (path) => apiRequest(path, { method: 'GET' }),
  post: (path, body) => apiRequest(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => apiRequest(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (path, body) => apiRequest(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path) => apiRequest(path, { method: 'DELETE' }),
};

export default api;
