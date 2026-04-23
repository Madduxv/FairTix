import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import Logo from '../components/Logo';
import Button from '../components/ui/Button';
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
            <Button variant="primary" size="lg" onClick={() => navigate('/events')}>Browse Events</Button>
            <Button variant="ghost" size="lg" onClick={() => navigate('/dashboard')}>Dashboard</Button>
          </>
        ) : (
          <>
            <Button variant="primary" size="lg" onClick={() => navigate('/login')}>Log In</Button>
            <Button variant="primary" size="lg" onClick={() => navigate('/signup')}>Sign Up</Button>
            <Button variant="ghost" size="lg" onClick={() => navigate('/events')}>Continue as guest</Button>
          </>
        )}
      </div>
    </div>
  );
}

export default Home;
