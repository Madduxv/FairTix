import { useState } from 'react';
import Layout from '../components/Layout';
import { useNavigate } from 'react-router-dom';

function Login() {
   const [email, setEmail] = useState('');
   const [password, setPassword] = useState('');
   const [error, setError] = useState('');
   let navigate = useNavigate();

   const handleSubmit = (e) => {
      e.preventDefault();
      setError('');

      if (!email || !password) {
         setError('Please fill in all fields.');
         return;
      }

      // TODO: replace with actual API call
      navigate("/events");
   };

   return (
      <Layout>
         <h1>LOG IN</h1>
         <form onSubmit={handleSubmit}>
            <label>
               email: <input name="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </label>
            <br />
            <label>
               password: <input name="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </label>
            <br /><br />
            {error && <p style={{ color: 'red' }}>{error}</p>}
            <button type="submit">log in</button>
         </form>
      </Layout>
   )
}

export default Login;
