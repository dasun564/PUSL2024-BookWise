package lk.ac.nsbm.bookwise.controller;

import lk.ac.nsbm.bookwise.service.LibraryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Student-facing catalogue screens.
 *
 * Layer: @Controller. Every method does the same three things - read the
 * request, call the service, name a view. No rule is evaluated here and no
 * repository is reached from here.
 */
@Controller
public class BookController {

    private final LibraryService libraryService;

    public BookController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/books")
    public String list(@RequestParam(value = "q", required = false) String query,
                       Authentication authentication,
                       Model model) {
        model.addAttribute("books", libraryService.searchBooks(query));
        model.addAttribute("categories", libraryService.listCategories());
        model.addAttribute("query", query);
        model.addAttribute("eligibility", libraryService.describeEligibility(authentication.getName()));
        return "books/list";
    }

    @GetMapping("/books/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("book", libraryService.getBook(id));
        model.addAttribute("eligibility", libraryService.describeEligibility(authentication.getName()));
        return "books/detail";
    }
}
