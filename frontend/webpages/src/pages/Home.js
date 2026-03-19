import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import useDeviceSize from '../UseDeviceSize';
import logo from '../logo.png';

function Home() {
  const [width] = useDeviceSize();
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <div>
      <div style={{ marginTop: '3em' }}>
        <img width={width} src={logo} alt="FairTix logo" />
      </div>
      <div>
        {user ? (
          <>
            <button onClick={() => navigate('/events')}>Browse Events</button>{' '}
            <button onClick={() => navigate('/dashboard')}>Dashboard</button>
          </>
        ) : (
          <>
            <button onClick={() => navigate('/login')}>Log In</button>{' '}
            <button onClick={() => navigate('/signup')}>Sign Up</button>{' '}
            <button onClick={() => navigate('/events')}>Continue as guest</button>
          </>
        )}
      </div>
    </div>
  );
}

export default Home;
