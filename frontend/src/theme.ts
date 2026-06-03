import { createTheme } from '@mui/material'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0F766E',
      dark: '#115E59',
      light: '#5EEAD4',
    },
    secondary: {
      main: '#F59E0B',
    },
    background: {
      default: '#F5F7F2',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#182026',
      secondary: '#52606D',
    },
  },
  shape: {
    borderRadius: 18,
  },
  typography: {
    fontFamily: '"Source Sans 3", sans-serif',
    h1: {
      fontFamily: '"Space Grotesk", sans-serif',
      fontWeight: 700,
    },
    h2: {
      fontFamily: '"Space Grotesk", sans-serif',
      fontWeight: 700,
    },
    h3: {
      fontFamily: '"Space Grotesk", sans-serif',
      fontWeight: 700,
    },
    h4: {
      fontFamily: '"Space Grotesk", sans-serif',
      fontWeight: 700,
    },
    h5: {
      fontFamily: '"Space Grotesk", sans-serif',
      fontWeight: 700,
    },
    h6: {
      fontFamily: '"Space Grotesk", sans-serif',
      fontWeight: 700,
    },
  },
  components: {
    MuiPaper: {
      styleOverrides: {
        root: {
          border: '1px solid rgba(15, 118, 110, 0.08)',
          boxShadow: '0 18px 40px rgba(24, 32, 38, 0.06)',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 999,
          paddingInline: 18,
          fontWeight: 700,
        },
      },
    },
  },
})
