import { render } from '@testing-library/react';
import App from './App';

test('renders App without crashing', () => {
  expect(() => render(<App />)).not.toThrow();
});
