package lk.ac.nsbm.bookwise.controller;

import jakarta.validation.Valid;
import lk.ac.nsbm.bookwise.dto.BookForm;
import lk.ac.nsbm.bookwise.dto.BookView;
import lk.ac.nsbm.bookwise.service.BookAdminService;
import lk.ac.nsbm.bookwise.service.LibraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * RESTful API over the same catalogue, satisfying the C1 requirement for CRUD
 * through REST controllers alongside the Thymeleaf interface.
 *
 * It calls the identical service beans as the web screens, so the business
 * rules - ISBN uniqueness, soft delete, stock consistency - cannot drift apart
 * between the two front ends. Failures are translated to JSON by
 * RestExceptionHandler; there is no try/catch here either.
 *
 * Reads require any authenticated user; writes require ROLE_ADMIN, enforced by
 * SecurityConfig and again by @PreAuthorize on BookAdminService.
 */
@RestController
@RequestMapping("/api/books")
public class BookRestController {

    private final LibraryService libraryService;
    private final BookAdminService bookAdminService;

    public BookRestController(LibraryService libraryService, BookAdminService bookAdminService) {
        this.libraryService = libraryService;
        this.bookAdminService = bookAdminService;
    }

    @GetMapping
    public List<BookView> search(@RequestParam(value = "q", required = false) String query) {
        return libraryService.searchBooks(query);
    }

    @GetMapping("/{id}")
    public BookView getOne(@PathVariable Long id) {
        return libraryService.getBook(id);
    }

    @PostMapping
    public ResponseEntity<BookView> create(@Valid @RequestBody BookForm form,
                                           UriComponentsBuilder uriBuilder) {
        BookView created = BookView.from(bookAdminService.create(form));
        return ResponseEntity
                .created(uriBuilder.path("/api/books/{id}").build(created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public BookView update(@PathVariable Long id, @Valid @RequestBody BookForm form) {
        return BookView.from(bookAdminService.update(id, form));
    }

    /** Soft delete, matching the web interface and the Part C sequence diagram. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookAdminService.softDeleteBook(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
