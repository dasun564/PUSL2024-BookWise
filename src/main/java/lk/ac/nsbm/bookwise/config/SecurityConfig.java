package lk.ac.nsbm.bookwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Authentication, role-based authorisation and session management.
 *
 * Roles: ROLE_STUDENT and ROLE_ADMIN, derived polymorphically from
 * AppUser.getRole() by AppUserDetailsService.
 *
 * {@code @EnableMethodSecurity} switches on the @PreAuthorize annotation used
 * on BookAdminService, so authorisation is enforced at two independent levels:
 * the URL here, and the service method itself.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Passwords are stored as BCrypt hashes, never in plain text. BCrypt is
     * deliberately slow and salts each hash, so two users with the same
     * password get different stored values and offline cracking is expensive.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * No AuthenticationProvider bean is declared here on purpose.
     *
     * Spring Boot builds a DaoAuthenticationProvider automatically from the
     * two beans this application already publishes: the AppUserDetailsService
     * component and the PasswordEncoder above. That is exactly the wiring an
     * explicit bean would have produced. Declaring one as well would replace
     * the auto-configured global AuthenticationManager and log a warning at
     * every start-up.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // --- public ---
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // H2 console is permitted so the required database screenshot
                // can be taken. In a production deployment this would be
                // removed entirely - it is a coursework evidence requirement.
                .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()

                // --- staff only ---
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/books/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("ADMIN")

                // --- students only ---
                .requestMatchers("/borrow/**", "/return/**", "/my-books").hasRole("STUDENT")

                // --- any signed-in user ---
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?loggedOut")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                // A signed-in user who reaches a page their role forbids gets
                // an explanatory page, not a bare 403.
                .accessDeniedPage("/403")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.migrateSession())
                .maximumSessions(1)
            )
            // CSRF stays ON for the whole application. It is disabled only for
            // the H2 console, which is a developer tool that posts its own
            // forms and cannot supply Spring's token.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**"))
            )
            // The H2 console renders inside a frameset, which the default
            // X-Frame-Options: DENY header would block.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}
