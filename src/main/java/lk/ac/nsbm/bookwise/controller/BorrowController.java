package lk.ac.nsbm.bookwise.controller;

import lk.ac.nsbm.bookwise.dto.BorrowResultView;
import lk.ac.nsbm.bookwise.dto.BorrowingView;
import lk.ac.nsbm.bookwise.service.LibraryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * The borrow and return workflow.
 *
 * Layer: @Controller. Note what is absent - there is no try/catch anywhere in
 * this class. Failures propagate out of the service as BookWiseException
 * subclasses and are rendered centrally by GlobalExceptionHandler.
 */
@Controller
public class BorrowController {

    private final LibraryService libraryService;

    public BorrowController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * Borrow a book.
     *
     * SECURITY: the only thing the browser sends is {@code bookId} in the URL.
     * The borrower's identity comes from {@code authentication.getName()},
     * which Spring Security read from the server-side session - it is never
     * taken from a form field, a hidden input or a request parameter.
     *
     * If the student id came from the request, any student could edit it in
     * browser devtools and borrow books onto a classmate's account: an
     * insecure direct object reference, i.e. horizontal privilege escalation.
     * They could exhaust someone else's 3-book allowance, or saddle them with
     * a loan that later becomes overdue and blocks their borrowing entirely.
     */
    @PostMapping("/borrow/{bookId}")
    public String borrow(@PathVariable Long bookId, Authentication authentication, Model model) {
        BorrowResultView result = libraryService.borrowBook(authentication.getName(), bookId);
        model.addAttribute("result", result);
        return "borrow/confirm";
    }

    @PostMapping("/return/{borrowingId}")
    public String returnBook(@PathVariable Long borrowingId, Authentication authentication, Model model) {
        BorrowingView result = libraryService.returnBook(authentication.getName(), borrowingId);
        model.addAttribute("returned", result);
        model.addAttribute("borrowings", libraryService.listMyBorrowings(authentication.getName()));
        model.addAttribute("eligibility", libraryService.describeEligibility(authentication.getName()));
        return "borrow/my-books";
    }

    @GetMapping("/my-books")
    public String myBooks(Authentication authentication, Model model) {
        model.addAttribute("borrowings", libraryService.listMyBorrowings(authentication.getName()));
        model.addAttribute("eligibility", libraryService.describeEligibility(authentication.getName()));
        return "borrow/my-books";
    }
}
