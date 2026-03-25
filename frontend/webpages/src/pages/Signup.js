import { useState } from 'react';
import Layout from '../components/Layout';
import { useNavigate } from 'react-router-dom';

function Signup() {
   const [email, setEmail] = useState('');
   const [phone, setPhone] = useState('');
   const [birthday, setBirthday] = useState('');
   const [password, setPassword] = useState('');
   const [confirmPassword, setConfirmPassword] = useState('');
   const [error, setError] = useState('');
   let navigate = useNavigate();

   const handleSubmit = (e) => {
      e.preventDefault();
      setError('');

      if (!email || !password || !confirmPassword || !birthday) {
         setError('Please fill in all required fields.');
         return;
      }

      if (password !== confirmPassword) {
         setError('Passwords do not match.');
         return;
      }

      const birthDate = new Date(birthday);
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();
      const monthDiff = today.getMonth() - birthDate.getMonth();
      if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
         age--;
      }
      if (age < 18) {
         setError('You must be at least 18 years old to sign up.');
         return;
      }

      // TODO: replace with actual API call
      navigate("/login");
   };

   return (
      <Layout>
         <h1>SIGN UP</h1>
         <form onSubmit={handleSubmit}>
            <label>
               email: <input name="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            </label>
            <br />
            <label>
               phone number: <input name="phone" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
            </label>
            <br />
            <label>
               birthday: <input name="birthday" type="date" value={birthday} onChange={(e) => setBirthday(e.target.value)} />
            </label>
            <br />
            <label>
               password: <input name="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </label>
            <br />
            <label>
               confirm password: <input name="confirmpassword" type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
            </label>
            <br /><br />
            {error && <p style={{ color: 'red' }}>{error}</p>}
            <button type="submit">sign up</button>
         </form>
      </Layout>
   )
}

export default Signup;
