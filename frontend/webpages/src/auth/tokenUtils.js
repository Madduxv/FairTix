const API_BASE = process.env.REACT_APP_API_URL || '';

/**
 * Fetches the current user from /auth/me using the HTTP-only cookie.
 * Returns { userId, email, role } or null if not authenticated.
 */
export async function fetchCurrentUser() {
  try {
    const response = await fetch(API_BASE + '/auth/me', {
      method: 'GET',
      credentials: 'include',
    });
    if (!response.ok) return null;
    return await response.json();
  } catch {
    return null;
  }
}
