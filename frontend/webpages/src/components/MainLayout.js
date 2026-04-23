import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import Footer from './Footer';
import ErrorBoundary from './ErrorBoundary';
import { ToastProvider } from './ui/Toast';
import '../styles/MainLayout.css';

function MainLayout() {
  return (
    <ToastProvider>
      <div className="main-layout">
        <a href="#main-content" className="skip-to-content">Skip to main content</a>
        <Navbar />
        <main id="main-content" className="main-content">
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </main>
        <Footer />
      </div>
    </ToastProvider>
  );
}

export default MainLayout;
