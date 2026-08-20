package lk.ac.nsbm.bookwise.service;

import lk.ac.nsbm.bookwise.dto.BookForm;
import lk.ac.nsbm.bookwise.entity.Book;
import lk.ac.nsbm.bookwise.entity.BorrowStatus;
import lk.ac.nsbm.bookwise.entity.EBook;
import lk.ac.nsbm.bookwise.entity.PrintedBook;
import lk.ac.nsbm.bookwise.exception.BookNotFoundException;
import lk.ac.nsbm.bookwise.exception.DuplicateIsbnException;
import lk.ac.nsbm.bookwise.repository.BookRepository;
import lk.ac.nsbm.bookwise.repository.BorrowingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catalogue maintenance, restricted to staff.
 *
 * Layer: @Service. Holds the catalogue rules (ISBN uniqueness, how a delete is
 * performed, how stock corrections keep availableCopies consistent) so that
 * both the Thymeleaf admin screens and the REST API get identical behaviour.
 *
 * {@code @PreAuthorize} repeats the ADMIN requirement that SecurityConfig
 * already enforces on /admin/**. That duplication is deliberate defence in
 * depth: if a future controller is added and someone forgets to protect its
 * URL, the service still refuses.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class BookAdminService {

    private static final Logger log = LoggerFactory.getLogger(BookAdminService.class);

    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;

    public BookAdminService(BookRepository bookRepository, BorrowingRepository borrowingRepository) {
        this.bookRepository = bookRepository;
        this.borrowingRepository = borrowingRepository;
    }

    @Transactional(readOnly = true)
    public List<Book> listAll() {
        return bookRepository.findAll(org.springframework.data.domain.Sort.by("title"));
    }

    @Transactional(readOnly = true)
    public Book getById(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public BookForm loadFormFor(Long id) {
        Book book = getById(id);
        BookForm form = new BookForm();
        form.setId(book.getId());
        form.setTitle(book.getTitle());
        form.setAuthor(book.getAuthor());
        form.setIsbn(book.getIsbn());
        form.setCategory(book.getCategory());
        form.setTotalCopies(book.getTotalCopies());
        if (book instanceof EBook ebook) {
            form.setFormat("EBOOK");
            form.setFileSizeMb(ebook.getFileSizeMb());
            form.setDownloadUrl(ebook.getDownloadUrl());
        } else if (book instanceof PrintedBook printed) {
            form.setFormat("PRINTED");
            form.setShelfLocation(printed.getShelfLocation());
            form.setBookCondition(printed.getBookCondition());
        }
        return form;
    }

    /**
     * Creates a new catalogue entry. A new book starts with every copy on the
     * shelf, so availableCopies is derived from totalCopies rather than being
     * accepted from the form.
     */
    @Transactional
    public Book create(BookForm form) {
        if (bookRepository.existsByIsbnAndActiveTrue(form.getIsbn())) {
            throw new DuplicateIsbnException(form.getIsbn());
        }

        int copies = form.getTotalCopies();
        Book book = "EBOOK".equals(form.getFormat())
                ? new EBook(form.getTitle(), form.getAuthor(), form.getIsbn(), form.getCategory(),
                            copies, copies, form.getFileSizeMb(), form.getDownloadUrl())
                : new PrintedBook(form.getTitle(), form.getAuthor(), form.getIsbn(), form.getCategory(),
                                  copies, copies, form.getShelfLocation(), form.getBookCondition());

        Book saved = bookRepository.save(book);
        log.info("Admin created book {} ('{}')", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * Updates an existing entry.
     *
     * The format (EBook vs PrintedBook) is intentionally NOT editable. It is
     * the JPA discriminator, and changing it would mean deleting and
     * re-inserting the row under a new identity, which would orphan every
     * Borrowing that references it. The edit form renders it read-only.
     */
    @Transactional
    public Book update(Long id, BookForm form) {
        Book book = getById(id);

        if (!book.getIsbn().equals(form.getIsbn())
                && bookRepository.existsByIsbnAndActiveTrue(form.getIsbn())) {
            throw new DuplicateIsbnException(form.getIsbn());
        }

        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());
        book.setCategory(form.getCategory());
        book.adjustTotalCopies(form.getTotalCopies());

        if (book instanceof EBook ebook) {
            ebook.setFileSizeMb(form.getFileSizeMb());
            ebook.setDownloadUrl(form.getDownloadUrl());
        } else if (book instanceof PrintedBook printed) {
            printed.setShelfLocation(form.getShelfLocation());
            printed.setBookCondition(form.getBookCondition());
        }

        log.info("Admin updated book {} ('{}')", book.getId(), book.getTitle());
        return book;
    }

    /**
     * SOFT DELETE - the decision recorded in the Part C admin-delete sequence
     * diagram.
     *
     * The row is never physically removed. Borrowing rows carry a foreign key
     * to book_id, and a hard delete would either fail against that constraint
     * or, with a cascade, silently erase every historical loan of the title -
     * destroying exactly the audit trail a library needs. Setting active=false
     * removes the book from search and from the student catalogue while every
     * Borrowing record and its foreign key stay intact, and a book still out
     * on loan can still be returned normally.
     */
    @Transactional
    public void softDeleteBook(Long id) {
        Book book = getById(id);
        long activeLoans = borrowingRepository.countByBookAndStatus(book, BorrowStatus.ACTIVE);

        book.setActive(false);

        log.info("Admin soft-deleted book {} ('{}'); {} active loan(s) preserved",
                book.getId(), book.getTitle(), activeLoans);
    }

    @Transactional
    public void restoreBook(Long id) {
        Book book = getById(id);
        book.setActive(true);
        log.info("Admin restored book {} ('{}')", book.getId(), book.getTitle());
    }

    @Transactional(readOnly = true)
    public long countActiveLoans(Book book) {
        return borrowingRepository.countByBookAndStatus(book, BorrowStatus.ACTIVE);
    }
}
