import { useState, useEffect } from 'react';

// this code isn't mine! it's from https://medium.com/@ismoil.793 -skylar

const useDeviceSize = () => {
   const [width, setWidth] = useState(0);
   const [height, setHeight] = useState(0);
   const handleWindowResize = () => {
      setWidth(window.innerWidth);
      setHeight(window.innerHeight);
   };
   useEffect(() => {
      // component is mounted and window is available
      handleWindowResize();
      window.addEventListener('resize', handleWindowResize);
      // unsubscribe from the event on component unmount
      return () => window.removeEventListener('resize', handleWindowResize);
   }, []);
   return [width, height];
};
export default useDeviceSize;
