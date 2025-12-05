// AdminLogin.js
// Simple login form for admin access with demo credentials.
import React, { useState } from 'react';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import Paper from '@mui/material/Paper';
import Divider from '@mui/material/Divider';
import InfoIcon from '@mui/icons-material/Info';

function AdminLogin({ onLogin, error: propError }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [localError, setLocalError] = useState('');

  const DEMO_CREDENTIALS = {
    username: 'admin',
    password: 'admin123'
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLocalError('');
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (res.ok) {
        const data = await res.json();
        onLogin(data.user);
      } else {
        setLocalError('Invalid credentials');
      }
    } catch {
      setLocalError('Login failed');
    }
  };

  const fillDemoCredentials = () => {
    setUsername(DEMO_CREDENTIALS.username);
    setPassword(DEMO_CREDENTIALS.password);
  };

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 300 }}>
      <Typography variant="h5" component="h2" textAlign="center" gutterBottom>
        Admin Login
      </Typography>

      {/* Demo Credentials Info */}
      <Paper
        elevation={0}
        sx={{
          p: 2,
          bgcolor: '#e3f2fd',
          border: '1px solid #90caf9',
          borderRadius: 2
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
          <InfoIcon color="primary" fontSize="small" />
          <Typography variant="subtitle2" color="primary" fontWeight={600}>
            Demo Credentials
          </Typography>
        </Box>
        <Divider sx={{ my: 1 }} />
        <Typography variant="body2" sx={{ fontFamily: 'monospace', mb: 0.5 }}>
          <strong>Username:</strong> {DEMO_CREDENTIALS.username}
        </Typography>
        <Typography variant="body2" sx={{ fontFamily: 'monospace', mb: 1 }}>
          <strong>Password:</strong> {DEMO_CREDENTIALS.password}
        </Typography>
        <Button
          variant="outlined"
          size="small"
          fullWidth
          onClick={fillDemoCredentials}
          sx={{ mt: 1 }}
        >
          Auto-fill Demo Credentials
        </Button>
      </Paper>

      <TextField
        label="Username"
        variant="outlined"
        value={username}
        onChange={e => setUsername(e.target.value)}
        required
        fullWidth
      />

      <TextField
        label="Password"
        type="password"
        variant="outlined"
        value={password}
        onChange={e => setPassword(e.target.value)}
        required
        fullWidth
      />

      <Button type="submit" variant="contained" color="primary" size="large" fullWidth>
        Login
      </Button>

      {(localError || propError) && <Alert severity="error">{localError || propError}</Alert>}
    </Box>
  );
}

export default AdminLogin;

