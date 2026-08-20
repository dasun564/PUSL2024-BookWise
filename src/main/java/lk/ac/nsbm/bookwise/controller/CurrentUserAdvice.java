package lk.ac.nsbm.bookwise.controller;

import lk.ac.nsbm.bookwise.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Puts the signed-in user's real name into the model for every Thymeleaf page,
 * so the navigation bar can greet them by name rather than by username.
 *
 * Scoped by assignableTypes to the MVC controllers only - the REST controller
 * has no model to populate.
 */
@ControllerAdvice(assignableTypes = {
        AuthController.class,
        BookController.class,
        BorrowController.class,
        AdminBookController.class
})
public class CurrentUserAdvice {

    private final AppUserRepository appUserRepository;

    public CurrentUserAdvice(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @ModelAttribute("currentUserFullName")
    public String currentUserFullName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return appUserRepository.findByUsername(authentication.getName())
                .map(user -> user.getFullName())
                .orElse(authentication.getName());
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
