package com.tradingapp.auth;

import com.tradingapp.auth.dto.AuthResponse;
import com.tradingapp.auth.dto.LoginRequest;
import com.tradingapp.auth.dto.RegisterRequest;
import com.tradingapp.auth.dto.UserResponse;
import com.tradingapp.common.ApiException;
import com.tradingapp.common.NotFoundException;
import com.tradingapp.security.JwtService;
import com.tradingapp.user.Role;
import com.tradingapp.user.User;
import com.tradingapp.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "email already registered");
        }
        User user = new User(
                request.username().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.USER);
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Throws BadCredentialsException -> 401 via the global handler.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = requireUser(email);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return UserResponse.from(requireUser(email));
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("user not found"));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationMs(),
                UserResponse.from(user));
    }
}
