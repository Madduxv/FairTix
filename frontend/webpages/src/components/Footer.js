import React from 'react';
import { Link } from 'react-router-dom';
import '../styles/MainLayout.css';

function Footer() {
  return (
    <footer className="site-footer">
      <div className="footer-content">
        <span>&copy; {new Date().getFullYear()} FairTix</span>
        <nav className="footer-links">
          <Link to="/privacy">Privacy Policy</Link>
          <Link to="/events">Events</Link>
        </nav>
      </div>
    </footer>
  );
}

export default Footer;
