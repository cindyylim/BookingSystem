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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

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
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3_600_000L);
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "jwtService", jwtUtil);
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

        User user = new User();
        user.setUsername("testuser");
        when(request.getCookies()).thenReturn(new Cookie[] { cookie });
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(user));

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

    @Test
    public void testFilterWithNonJwtCookies() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(new Cookie[] {
                new Cookie("session", "abc"),
                new Cookie("other", "xyz")
        });

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userService, never()).getUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testFilterWithValidTokenButUserMissing() throws ServletException, IOException {
        String token = jwtUtil.generateToken("ghost");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("jwt", token) });
        when(userService.getUserByUsername("ghost")).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testFilterValidateTokenFalseAfterExtract() throws ServletException, IOException {
        JwtService mockJwt = mock(JwtService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mockJwt, userService);
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("jwt", "extracted-but-invalid") });
        when(mockJwt.extractUsername("extracted-but-invalid")).thenReturn("testuser");
        when(mockJwt.validateToken("extracted-but-invalid")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userService, never()).getUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testFilterAlreadyAuthenticated() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken existing = new UsernamePasswordAuthenticationToken("existing", null, null);
        SecurityContextHolder.getContext().setAuthentication(existing);
        String token = jwtUtil.generateToken("otheruser");
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("jwt", token) });

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertEquals("existing", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(userService, never()).getUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testJwtUtilExtractUsernameMalformedToken() {
        assertNull(jwtUtil.extractUsername("not-a-jwt"));
        assertNull(jwtUtil.extractUsername("a.b.c"));
        assertNull(jwtUtil.extractUsername(""));
    }

    @Test
    public void testJwtUtilExpiredToken() {
        String secret = "v9y$B&E)H@McQfTjWmZq4t7w!z%C*F-JaNdRgUkXp2s5u8x/A?D(G+KbPeShVmYp";
        Date issued = new Date(System.currentTimeMillis() - 120_000);
        Date expired = new Date(System.currentTimeMillis() - 60_000);
        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(issued)
                .setExpiration(expired)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .compact();

        assertFalse(jwtUtil.validateToken(token));
        assertNull(jwtUtil.extractUsername(token));
    }
}
