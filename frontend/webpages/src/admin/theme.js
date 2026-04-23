import { createTheme } from '@mui/material/styles';

// MUI themes require static values at creation time — CSS vars can't be used here.
// Each value is annotated with its corresponding tokens.css dark-mode token.
// When tokens.css dark-mode values change, update the matching lines below.
const T = {
  bgPage:       '#0d1117', // --bg-page (dark)
  surface:      '#1a1a2e', // --surface (dark)
  surfaceRaised:'#16213e', // --surface-raised (dark)
  textPrimary:  '#ffffff', // --text-primary (dark)
  textSecondary:'#b0b0b0', // --text-secondary (dark)
  brandPrimary: '#e94560', // --brand-primary
  brandAccent:  '#0f3460', // --brand-accent
};

const adminTheme = createTheme({
  palette: {
    mode: 'dark',
    primary:    { main: T.brandPrimary },
    secondary:  { main: T.brandAccent },
    background: { default: T.bgPage, paper: T.surfaceRaised },
    text:       { primary: T.textPrimary, secondary: T.textSecondary },
  },
  typography: {
    fontFamily: "'Roboto', 'Arial', sans-serif",
  },
  components: {
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: T.surfaceRaised,
          borderRight: '1px solid rgba(255, 255, 255, 0.08)',
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontWeight: 600,
          backgroundColor: T.brandAccent,
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: { textTransform: 'none' },
      },
    },
  },
});

export default adminTheme;
