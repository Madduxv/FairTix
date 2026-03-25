import { jwtDecode } from 'jwt-decode';

const TOKEN_KEY = 'fairtix_token';

export function saveToken(token) {
  sessionStorage.setItem(TOKEN_KEY, token);
}

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function removeToken() {
  sessionStorage.removeItem(TOKEN_KEY);
}

export function decodeToken(token) {
  try {
    return jwtDecode(token);
  } catch {
    return null;
  }
}

export function isTokenExpired(token) {
  const decoded = decodeToken(token);
  if (!decoded || !decoded.exp) return true;
  return decoded.exp < Math.floor(Date.now() / 1000);
}

export function getAuthPayload() {
  const token = getToken();
  if (!token) return null;

  if (isTokenExpired(token)) {
    removeToken();
    return null;
  }

  const decoded = decodeToken(token);
  if (!decoded) return null;

  return {
    token,
    email: decoded.sub,
    userId: decoded.userId,
    role: decoded.role,
  };
}
