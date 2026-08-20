package lk.ac.nsbm.bookwise.controller;

import jakarta.validation.Valid;
import lk.ac.nsbm.bookwise.dto.BookForm;
import lk.ac.nsbm.bookwise.exception.DuplicateIsbnException;
import lk.ac.nsbm.bookwise.service.BookAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Catalogue management screens. Reachable only with ROLE_ADMIN - enforced by
 * SecurityConfig on the /admin/** path and again by @PreAuthorize on
 * BookAdminService.
 *
 * Layer: @Controller. It decides which view to render and re-displays the form
 * when Bean Validation rejects the input; it never decides what a valid book is.
 */
@Controller
@RequestMapping("/admin/books")
public class AdminBookController {

    private final BookAdminService bookAdminService;

    public AdminBookController(BookAdminService bookAdminService) {
        this.bookAdminService = bookAdminService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("books", bookAdminService.listAll());
        return "admin/book-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("bookForm", new BookForm());
        model.addAttribute("editing", false);
        return "admin/book-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("bookForm") BookForm bookForm,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            return "admin/book-form";
        }
        try {
            var saved = bookAdminService.create(bookForm);
            redirect.addFlashAttribute("flash", "Added \"" + saved.getTitle() + "\" to the catalogue.");
            return "redirect:/admin/books";
        } catch (DuplicateIsbnException ex) {
            // Attached to the offending field so the message appears next to
            // the ISBN box rather than as a page-level error.
            binding.rejectValue("isbn", "duplicate", ex.getUserMessage());
            model.addAttribute("editing", false);
            return "admin/book-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("bookForm", bookAdminService.loadFormFor(id));
        model.addAttribute("editing", true);
        return "admin/book-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("bookForm") BookForm bookForm,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", true);
            return "admin/book-form";
        }
        try {
            var saved = bookAdminService.update(id, bookForm);
            redirect.addFlashAttribute("flash", "Updated \"" + saved.getTitle() + "\".");
            return "redirect:/admin/books";
        } catch (DuplicateIsbnException ex) {
            binding.rejectValue("isbn", "duplicate", ex.getUserMessage());
            model.addAttribute("editing", true);
            return "admin/book-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        var book = bookAdminService.getById(id);
        String title = book.getTitle();
        bookAdminService.softDeleteBook(id);
        redirect.addFlashAttribute("flash",
                "Withdrew \"" + title + "\" from the catalogue. Borrowing history has been preserved.");
        return "redirect:/admin/books";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirect) {
        bookAdminService.restoreBook(id);
        redirect.addFlashAttribute("flash", "Restored the book to the catalogue.");
        return "redirect:/admin/books";
    }
}
