import './App.css';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import MainLayout from './components/MainLayout';
import ProtectedRoute from './components/ProtectedRoute';
import AdminRoute from './components/AdminRoute';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Events from './pages/Events';
import EventDetail from './pages/EventDetail';
import Dashboard from './pages/Dashboard';
import MyTickets from './pages/MyTickets';
import MyHolds from './pages/MyHolds';
import Checkout from './pages/Checkout';
import PrivacyPolicy from './pages/PrivacyPolicy';
import AdminLayout from './admin/AdminLayout';
import AdminDashboard from './admin/pages/AdminDashboard';
import AdminEventsPage from './admin/pages/AdminEventsPage';
import AdminSeatsPage from './admin/pages/AdminSeatsPage';
import AdminUsersPage from './admin/pages/AdminUsersPage';

function SessionExpiredBanner() {
  const { sessionExpired, clearSessionExpired } = useAuth();
  if (!sessionExpired) return null;
  return (
    <div className="session-expired-banner" role="alert">
      <span>Your session has expired. Please log in again.</span>
      <button onClick={clearSessionExpired} className="session-expired-dismiss">Dismiss</button>
    </div>
  );
}

function App() {
  return (
    <div className="App">
      <Router>
        <AuthProvider>
          <SessionExpiredBanner />
          <Routes>
            {/* Public & authenticated routes wrapped in MainLayout */}
            <Route element={<MainLayout />}>
              <Route path="/" element={<Home />} />
              <Route path="/events" element={<Events />} />
              <Route path="/events/:eventId" element={<EventDetail />} />
              <Route path="/login" element={<Login />} />
              <Route path="/signup" element={<Signup />} />
              <Route path="/privacy" element={<PrivacyPolicy />} />

              {/* Authenticated routes */}
              <Route element={<ProtectedRoute />}>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/my-holds" element={<MyHolds />} />
                <Route path="/checkout" element={<Checkout />} />
                <Route path="/my-tickets" element={<MyTickets />} />
              </Route>
            </Route>

            {/* Admin routes — own layout */}
            <Route element={<AdminRoute />}>
              <Route path="/admin" element={<AdminLayout />}>
                <Route index element={<AdminDashboard />} />
                <Route path="events" element={<AdminEventsPage />} />
                <Route path="events/:eventId/seats" element={<AdminSeatsPage />} />
                <Route path="users" element={<AdminUsersPage />} />
              </Route>
            </Route>

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </AuthProvider>
      </Router>
    </div>
  );
}

export default App;
