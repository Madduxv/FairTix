import React from 'react';
import { useNavigate } from 'react-router-dom';

function Home() {
   let navigate = useNavigate();
   return (
      <div>
         <br></br><br></br><br></br>
         <h1>WELCOME TO FAIRTIX [skylar will put a logo here later]</h1>
         <div><button onClick={() => { navigate("/login"); }}>Log in</button> <button onClick={() => { navigate("/signup"); }}>Sign up</button> <button>Continue as guest</button></div>
      </div>
   );
}

export default Home;