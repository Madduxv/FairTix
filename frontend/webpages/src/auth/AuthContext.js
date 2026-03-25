import React, { createContext, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { saveToken, removeToken, getAuthPayload, decodeToken } from './tokenUtils';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  const logout = useCallback(() => {
    removeToken();
    setUser(null);
    setToken(null);
    navigate('/');
  }, [navigate]);

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
      logout();
      return;
    }

    const timer = setTimeout(logout, msUntilExpiry);
    return () => clearTimeout(timer);
  }, [token, logout]);

  async function login(email, password) {
    const data = await api.post('/auth/login', { email, password });
    const decoded = decodeToken(data.token);
    if (!decoded || !decoded.sub || !decoded.userId || !decoded.role) {
      throw new Error('Authentication failed: invalid token received from server.');
    }
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
