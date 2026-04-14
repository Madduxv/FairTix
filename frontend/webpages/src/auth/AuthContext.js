import React, { createContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { saveToken, removeToken, getAuthPayload, decodeToken } from './tokenUtils';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionExpired, setSessionExpired] = useState(false);
  const navigate = useNavigate();

  const logout = useCallback((expired = false) => {
    removeToken();
    setUser(null);
    setToken(null);
    if (expired) {
      setSessionExpired(true);
    }
    navigate('/');
  }, [navigate]);

  const clearSessionExpired = useCallback(() => {
    setSessionExpired(false);
  }, []);

  // Hydrate from localStorage on mount
  useEffect(() => {
    const payload = getAuthPayload();
    if (payload) {
      setToken(payload.token);
      setUser({
        email: payload.email,
        userId: payload.userId,
        role: payload.role,
      });
    }
    setIsLoading(false);
  }, []);

  // Auto-logout on token expiry
  useEffect(() => {
    if (!token) return;

    const decoded = decodeToken(token);
    if (!decoded || !decoded.exp) return;

    const msUntilExpiry = decoded.exp * 1000 - Date.now();
    if (msUntilExpiry <= 0) {
      logout(true);
      return;
    }

    const timer = setTimeout(() => logout(true), msUntilExpiry);
    return () => clearTimeout(timer);
  }, [token, logout]);

  async function login(email, password) {
    const data = await api.post('/auth/login', { email, password });
    const decoded = decodeToken(data.token);
    if (!decoded || !decoded.sub || !decoded.userId || !decoded.role) {
      throw new Error('Authentication failed: invalid token received from server.');
    }
    setSessionExpired(false);
    saveToken(data.token);
    setToken(data.token);
    const userInfo = {
      email: decoded.sub,
      userId: decoded.userId,
      role: decoded.role,
    };
    setUser(userInfo);
    return userInfo;
  }

  async function signup(email, password) {
    const data = await api.post('/auth/register', { email, password });
    const decoded = decodeToken(data.token);
    if (!decoded || !decoded.sub || !decoded.userId || !decoded.role) {
      throw new Error('Registration failed: invalid token received from server.');
    }
    setSessionExpired(false);
    saveToken(data.token);
    setToken(data.token);
    const userInfo = {
      email: decoded.sub,
      userId: decoded.userId,
      role: decoded.role,
    };
    setUser(userInfo);
    return userInfo;
  }

  const value = {
    user,
    token,
    isLoading,
    sessionExpired,
    clearSessionExpired,
    login,
    signup,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
