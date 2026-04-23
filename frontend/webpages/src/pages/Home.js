import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import Logo from '../components/Logo';
import '../styles/Home.css';

function Home() {
  useEffect(() => { document.title = 'FairTix — Fair-Access Ticketing'; }, []);
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="home-page">
      <Logo />
      <div className="home-actions">
        {user ? (
          <>
            <button onClick={() => navigate('/events')}>Browse Events</button>
            <button className="home-btn-secondary" onClick={() => navigate('/dashboard')}>Dashboard</button>
          </>
        ) : (
          <>
            <button onClick={() => navigate('/login')}>Log In</button>
            <button onClick={() => navigate('/signup')}>Sign Up</button>
            <button className="home-btn-secondary" onClick={() => navigate('/events')}>Continue as guest</button>
          </>
        )}
      </div>
    </div>
  );
}

export default Home;
