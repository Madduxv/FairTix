import React from 'react';
import { useAuth } from '../auth/useAuth';

function Dashboard() {
  const { user } = useAuth();

  return (
    <div style={{ padding: '2rem' }}>
      <h2>Dashboard</h2>
      <div style={{ marginTop: '1rem' }}>
        <p><strong>Email:</strong> {user.email}</p>
        <p><strong>Role:</strong> {user.role}</p>
        <p><strong>User ID:</strong> {user.userId}</p>
      </div>
    </div>
  );
}

export default Dashboard;
