import { getToken } from '../auth/tokenUtils';

const API_BASE = process.env.REACT_APP_API_URL || '';

async function apiRequest(path, options = {}) {
  const token = getToken();

  const headers = {
    ...options.headers,
  };

  if (options.body) {
    headers['Content-Type'] = 'application/json';
  }

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
      error = new Error(body.message || body.failureReason || 'Request failed');
      error.status = response.status;
      error.code = body.code;
      error.body = body;
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
