package com.example.booking.security;

import com.example.booking.model.User;
import com.example.booking.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecurityTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
        // Inject secret manually since it's @Value
        ReflectionTestUtils.setField(jwtUtil, "secretString",
                "v9y$B&E)H@McQfTjWmZq4t7w!z%C*F-JaNdRgUkXp2s5u8x/A?D(G+KbPeShVmYp");
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "jwtUtil", jwtUtil);
    }

    @Test
    public void testJwtUtil() {
        String token = jwtUtil.generateToken("testuser");
        assertNotNull(token);
        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    public void testJwtUtilExpired() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    public void testFilterWithValidToken() throws ServletException, IOException {
        String token = jwtUtil.generateToken("testuser");
        Cookie cookie = new Cookie("jwt", token);

        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(new User()));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("testuser", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testFilterWithNoCookie() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testFilterWithInvalidToken() throws ServletException, IOException {
        Cookie cookie = new Cookie("jwt", "invalid");
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
