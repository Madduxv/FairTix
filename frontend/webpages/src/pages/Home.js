import Logo from '../components/Logo';
import { useNavigate } from 'react-router-dom';

function Home() {
   let navigate = useNavigate();
   return (
      <div>
         <br /><br /><br />
         <Logo />
         <div>
            <button onClick={() => navigate("/login")}>Log in</button>{' '}
            <button onClick={() => navigate("/signup")}>Sign up</button>{' '}
            <button onClick={() => navigate("/events")}>Continue as guest</button>
         </div>
      </div>
   );
}

export default Home;
