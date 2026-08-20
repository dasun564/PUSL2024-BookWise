package lk.ac.nsbm.bookwise.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Login, landing and access-denied screens.
 *
 * Layer: @Controller. Chooses views only; it holds no business logic and
 * touches no repository.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Sends each role to the screen that is useful to it. Students land on the
     * catalogue, staff on the catalogue management table.
     */
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/books";
        }
        return "redirect:/books";
    }

    /**
     * Shown when an authenticated user reaches a page their role does not
     * allow - for example a student opening /admin/books.
     */
    @GetMapping("/403")
    public String accessDenied(HttpServletRequest request) {
        return "error/403";
    }
}
