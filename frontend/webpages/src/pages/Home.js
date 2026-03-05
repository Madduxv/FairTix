import React from 'react';
import { useNavigate } from 'react-router-dom';
import UseDeviceSize from '../UseDeviceSize'
import logo from '../logo.png';

function Home() {
   const [width, height] = UseDeviceSize();
   let navigate = useNavigate();
   return (
      <div>
         <br></br><br></br><br></br>
         <div> <img width={width} src={logo} alt="really cool logo, trust"/></div>
         <div><button onClick={() => { navigate("/login"); }}>Log in</button> <button onClick={() => { navigate("/signup"); }}>Sign up</button> <button>Continue as guest</button></div>
      </div>
   );
}

export default Home;