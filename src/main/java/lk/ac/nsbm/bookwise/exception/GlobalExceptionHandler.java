package lk.ac.nsbm.bookwise.exception;

import jakarta.servlet.http.HttpServletRequest;
import lk.ac.nsbm.bookwise.controller.AdminBookController;
import lk.ac.nsbm.bookwise.controller.AuthController;
import lk.ac.nsbm.bookwise.controller.BookController;
import lk.ac.nsbm.bookwise.controller.BorrowController;
import org.slf4j.Logger;
import lk.ac.nsbm.bookwise.repository.AppUserRepository;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * CENTRALISED ERROR HANDLING for the Thymeleaf screens.
 *
 * Spring mechanism: {@code @ControllerAdvice} combined with
 * {@code @ExceptionHandler}. Spring registers this class as a global handler
 * for every controller listed in {@code assignableTypes}, so an exception
 * thrown deep inside the service layer unwinds past the controller and is
 * caught here instead.
 *
 * Why this mechanism was chosen over try/catch in each controller method:
 *
 *  - The alternative is the same catch block copied into every handler
 *    method, which is duplicated code that drifts. Here the mapping from
 *    failure to page exists exactly once.
 *  - Controllers stay readable: BorrowController.borrow() is three lines
 *    describing the happy path, which is the whole point of the layer.
 *  - A try/catch inside a @Transactional call chain also invites the mistake
 *    of swallowing the exception and letting a half-finished transaction
 *    commit. Letting it propagate is what triggers the rollback.
 *  - New failure types are handled automatically: because every business
 *    failure extends BookWiseException and carries its own message, adding a
 *    fifth exception needs no change to this class at all.
 *
 * Each exception still produces its OWN specific wording - the handler is
 * shared, the message is not. The message text comes from the exception
 * instance, which holds the data (which book, how many copies, which due date)
 * needed to say something useful.
 */
@ControllerAdvice(assignableTypes = {
        AuthController.class,
        BookController.class,
        BorrowController.class,
        AdminBookController.class
})
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AppUserRepository appUserRepository;

    public GlobalExceptionHandler(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * Adds the signed-in user's name to an error view.
     *
     * Spring does not apply a @ModelAttribute method from CurrentUserAdvice to
     * a view produced by an @ExceptionHandler, so without this the navigation
     * bar on every error page would read "Signed in as" followed by nothing.
     */
    private void addCurrentUser(ModelAndView mav) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        mav.addObject("currentUsername", auth.getName());
        mav.addObject("currentUserFullName", appUserRepository.findByUsername(auth.getName())
                .map(user -> user.getFullName())
                .orElse(auth.getName()));
    }

    @ExceptionHandler(BookWiseException.class)
    public ModelAndView handleBusinessFailure(BookWiseException ex, HttpServletRequest request) {
        log.warn("Business rule rejected {} {}: [{}] {}",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        ModelAndView mav = new ModelAndView("borrow/error");
        mav.setStatus(ex.getHttpStatus());
        mav.addObject("errorTitle", ex.getTitle());
        mav.addObject("errorMessage", ex.getUserMessage());
        mav.addObject("errorAction", ex.getSuggestedAction());
        mav.addObject("errorCode", ex.getErrorCode());
        mav.addObject("ruleViolation", isPersonalisedRuleViolation(ex));
        addCurrentUser(mav);
        return mav;
    }

    /**
     * Marks the two failures that come from the personalised borrowing rule
     * for student 10965261, so the error page can label them as such.
     */
    private boolean isPersonalisedRuleViolation(BookWiseException ex) {
        return ex instanceof BorrowLimitExceededException || ex instanceof OverdueBookHeldException;
    }

    /**
     * Last-resort handler. Anything unforeseen still reaches a styled page
     * rather than a stack trace, and is logged at ERROR with the full cause.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);

        ModelAndView mav = new ModelAndView("borrow/error");
        mav.setStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "Something went wrong");
        mav.addObject("errorMessage", "The system could not complete that request.");
        mav.addObject("errorAction", "Please try again. If it keeps happening, contact the library desk.");
        mav.addObject("errorCode", "INTERNAL_ERROR");
        mav.addObject("ruleViolation", false);
        addCurrentUser(mav);
        return mav;
    }
}
