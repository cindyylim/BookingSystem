// AdminLogin.js
// Simple login form for admin access.
import React, { useState } from 'react';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';

function AdminLogin({ onLogin, error: propError }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [localError, setLocalError] = useState('');

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

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 300 }}>
      <Typography variant="h5" component="h2" textAlign="center" gutterBottom>
        Admin Login
      </Typography>

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

