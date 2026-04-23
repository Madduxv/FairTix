import React from 'react';
import './Button.css';

function Button({ variant = 'primary', size = 'md', className = '', children, ...props }) {
  return (
    <button className={`btn btn--${variant} btn--${size} ${className}`.trim()} {...props}>
      {children}
    </button>
  );
}

export default Button;
