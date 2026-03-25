import logo from '../logo.png';

// don't worry i'll make a better logo later! i have graphic design skills
// i was thinking something white and orange? -skylar
function Logo() {
   return (
      <div>
         <img
            style={{ width: '100%', maxWidth: '400px' }}
            src={logo}
            alt="really cool logo, trust"
         />
      </div>
   )
}

export default Logo;
