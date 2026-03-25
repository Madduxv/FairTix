import Logo from './Logo';
import { useNavigate } from 'react-router-dom';

function Layout({ children }) {
   let navigate = useNavigate();
   return (
      <>
         <nav>
            <button onClick={() => navigate("/")}>Home</button>{' '}
            <button onClick={() => navigate("/events")}>Events</button>{' '}
            <button onClick={() => navigate("/login")}>Log in</button>{' '}
            <button onClick={() => navigate("/signup")}>Sign up</button>
         </nav>
         <Logo />
         {children}
      </>
   )
}

export default Layout;
