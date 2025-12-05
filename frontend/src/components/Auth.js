// Auth.js
import React, { useState } from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import Grid from '@mui/material/Grid';

function Auth({ onAuth }) {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (isLogin) {
      // Login
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (res.ok) {
        const data = await res.json();
        onAuth(data.user);
      } else {
        setError('Invalid credentials');
      }
    } else {
      // Register
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, email, phone })
      });
      if (res.ok) {
        setIsLogin(true);
        setError(''); // Clear any previous errors
        alert('Registration successful! Please login.');
      } else {
        const errorData = await res.json();
        setError(errorData.error || 'Registration failed');
      }
    }
  };

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
      <Card elevation={4} sx={{ maxWidth: 450, width: '100%', borderRadius: 2 }}>
        <Box sx={{ bgcolor: 'primary.main', p: 3, textAlign: 'center' }}>
          <Typography variant="h5" color="white" fontWeight="600">
            {isLogin ? 'Welcome Back' : 'Create Account'}
          </Typography>
        </Box>
        <CardContent sx={{ p: 4 }}>
          <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="Username"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              fullWidth
              variant="outlined"
            />
            <TextField
              label="Password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              fullWidth
              variant="outlined"
            />

            {!isLogin && (
              <>
                <TextField
                  label="Email Address"
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                  fullWidth
                  variant="outlined"
                />
                <TextField
                  label="Phone Number"
                  type="tel"
                  value={phone}
                  onChange={e => setPhone(e.target.value)}
                  fullWidth
                  variant="outlined"
                />
              </>
            )}

            <Button
              type="submit"
              variant="contained"
              color="primary"
              size="large"
              fullWidth
              sx={{ mt: 1, py: 1.5, fontSize: '1rem' }}
            >
              {isLogin ? 'Login' : 'Register'}
            </Button>
          </Box>

          {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}

          <Box sx={{ mt: 3, textAlign: 'center' }}>
            <Button
              onClick={() => { setIsLogin(!isLogin); setError(''); }}
              color="secondary"
              sx={{ textTransform: 'none' }}
            >
              {isLogin ? "Don't have an account? Register" : "Already have an account? Login"}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}

export default Auth; 