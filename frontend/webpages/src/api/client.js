const API_BASE = process.env.REACT_APP_API_URL || '';

let isRefreshing = false;

async function apiRequest(path, options = {}, isRetry = false) {
  const headers = {
    ...options.headers,
  };

  if (options.body) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(API_BASE + path, {
    ...options,
    headers,
    credentials: 'include',
  });

  if ((response.status === 401 || response.status === 403) && !isRetry && path !== '/auth/refresh' && path !== '/auth/login') {
    if (isRefreshing) {
      throw buildError(response, 'Session expired');
    }
    isRefreshing = true;
    try {
      const refreshResponse = await fetch(API_BASE + '/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      });
      if (refreshResponse.ok) {
        isRefreshing = false;
        return apiRequest(path, options, true);
      }
    } catch (_) {
      // network error during refresh — fall through to logout
    }
    isRefreshing = false;
    // Refresh failed — signal session expiry to AuthContext via a custom event
    window.dispatchEvent(new CustomEvent('auth:session-expired'));
    throw buildError(response, 'Session expired');
  }

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

function buildError(response, message) {
  const error = new Error(message);
  error.status = response.status;
  return error;
}

const api = {
  get: (path) => apiRequest(path, { method: 'GET' }),
  post: (path, body) => apiRequest(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => apiRequest(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (path, body) => apiRequest(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path) => apiRequest(path, { method: 'DELETE' }),
};

export default api;
