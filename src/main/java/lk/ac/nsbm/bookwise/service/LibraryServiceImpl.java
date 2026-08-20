package lk.ac.nsbm.bookwise.service;

import lk.ac.nsbm.bookwise.dto.BookView;
import lk.ac.nsbm.bookwise.dto.BorrowEligibilityView;
import lk.ac.nsbm.bookwise.dto.BorrowResultView;
import lk.ac.nsbm.bookwise.dto.BorrowingView;
import lk.ac.nsbm.bookwise.entity.Book;
import lk.ac.nsbm.bookwise.entity.BorrowStatus;
import lk.ac.nsbm.bookwise.entity.Borrowing;
import lk.ac.nsbm.bookwise.entity.Student;
import lk.ac.nsbm.bookwise.exception.BookNotFoundException;
import lk.ac.nsbm.bookwise.exception.BorrowLimitExceededException;
import lk.ac.nsbm.bookwise.exception.BorrowingNotFoundException;
import lk.ac.nsbm.bookwise.exception.NoCopiesAvailableException;
import lk.ac.nsbm.bookwise.exception.OverdueBookHeldException;
import lk.ac.nsbm.bookwise.repository.BookRepository;
import lk.ac.nsbm.bookwise.repository.BorrowingRepository;
import lk.ac.nsbm.bookwise.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The only place in BookWise where borrowing rules are decided.
 *
 * Layer: @Service. It owns the transaction boundary and the business rules
 * that span more than one entity. Controllers above it do HTTP translation
 * only; repositories below it do data access only.
 */
@Service
public class LibraryServiceImpl implements LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryServiceImpl.class);

    // =======================================================================
    // PERSONALISED BORROWING RULE - student 10965261
    // Last digit of the student ID is 1, so band D = 0-4 applies:
    //   "A student may borrow a maximum of 3 books at a time, and a book
    //    cannot be borrowed if the student already has an overdue book."
    //
    // The 0-4 band does not specify a loan period, so 14 days is adopted and
    // declared as a stated assumption in the report.
    // =======================================================================
    private static final int BORROW_LIMIT = 3;
    private static final int LOAN_DAYS = 14;

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;

    /**
     * Constructor injection. Preferred over field injection because the
     * dependencies are final and the object cannot be constructed in an
     * incomplete state - which also makes the class unit-testable without a
     * Spring context.
     */
    public LibraryServiceImpl(StudentRepository studentRepository,
                              BookRepository bookRepository,
                              BorrowingRepository borrowingRepository) {
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
        this.borrowingRepository = borrowingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookView> searchBooks(String term) {
        if (term == null || term.isBlank()) {
            return listCatalogue();
        }
        return bookRepository.searchByTitleOrCategory(term.trim()).stream()
                .map(BookView::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookView> listCatalogue() {
        return bookRepository.findByActiveTrueOrderByTitleAsc().stream()
                .map(BookView::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return bookRepository.findDistinctCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public BookView getBook(Long bookId) {
        return bookRepository.findByIdAndActiveTrue(bookId)
                .map(BookView::from)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }

    /**
     * Borrow one copy of a book for the authenticated student.
     *
     * ATOMICITY: @Transactional wraps the whole method. Spring creates an AOP
     * proxy around this bean that opens a transaction on entry, commits on
     * normal return, and rolls back if any unchecked exception escapes. Every
     * BookWiseException is unchecked, so a rejection anywhere below undoes the
     * copy decrement automatically. Without it each repository call would
     * commit independently and a failure after the decrement would leave a
     * book with a copy subtracted and no borrowing record - stock lost forever.
     */
    @Override
    @Transactional
    public BorrowResultView borrowBook(String username, Long bookId) {
        Student student = requireStudent(username);
        LocalDate today = LocalDate.now();

        // GUARD ORDER MATTERS. Facts about the BOOK are checked first, then
        // facts about the STUDENT. Drawing the Part D activity diagram showed
        // that the original order was wrong: a student holding 3 books who
        // clicked a title that was fully on loan was told "you have reached
        // your borrowing limit" for a book they could not have borrowed
        // anyway. The message was true but it was not the reason the request
        // failed, and it pointed them at the wrong remedy.
        Book book = bookRepository.findByIdForUpdate(bookId)
                .filter(Book::isActive)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (book.getAvailableCopies() <= 0) {
            throw new NoCopiesAvailableException(book.getTitle(), book.getTotalCopies());
        }

        // --- PERSONALISED RULE, band 0-4 -----------------------------------
        long activeCount = borrowingRepository.countByStudentAndStatus(student, BorrowStatus.ACTIVE);
        if (activeCount >= BORROW_LIMIT) {
            throw new BorrowLimitExceededException(activeCount, BORROW_LIMIT);
        }

        Optional<Borrowing> overdue = borrowingRepository.findFirstOverdue(student, today);
        if (overdue.isPresent()) {
            Borrowing od = overdue.get();
            throw new OverdueBookHeldException(od.getBook().getTitle(), od.getDueDate(), today);
        }
        // --- end personalised rule ------------------------------------------

        book.decrementCopies();
        Borrowing borrowing = new Borrowing(student, book, today, today.plusDays(LOAN_DAYS));
        borrowingRepository.save(borrowing);

        log.info("Student {} borrowed book {} ('{}'), due {}",
                username, book.getId(), book.getTitle(), borrowing.getDueDate());

        return BorrowResultView.from(borrowing, LOAN_DAYS);
    }

    /**
     * Return a book. Also transactional: the stock increment and the status
     * change must either both happen or neither.
     */
    @Override
    @Transactional
    public BorrowingView returnBook(String username, Long borrowingId) {
        Student student = requireStudent(username);

        Borrowing borrowing = borrowingRepository.findByIdWithBookAndStudent(borrowingId)
                .orElseThrow(() -> new BorrowingNotFoundException(borrowingId));

        // Ownership check: a student may only return their own loan. Same
        // reasoning as taking the identity from the session - the borrowing id
        // arrives in the URL and must never be trusted on its own.
        if (!borrowing.getStudent().getId().equals(student.getId())) {
            throw new BorrowingNotFoundException(borrowingId);
        }

        LocalDate today = LocalDate.now();
        borrowing.markReturned(today);
        borrowing.getBook().incrementCopies();

        log.info("Student {} returned borrowing {} with status {}",
                username, borrowingId, borrowing.getStatus());

        return BorrowingView.from(borrowing, today);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowingView> listMyBorrowings(String username) {
        Student student = requireStudent(username);
        LocalDate today = LocalDate.now();
        return borrowingRepository.findAllByStudentWithBook(student).stream()
                .map(b -> BorrowingView.from(b, today))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BorrowEligibilityView describeEligibility(String username) {
        Student student = requireStudent(username);
        LocalDate today = LocalDate.now();

        long activeCount = borrowingRepository.countByStudentAndStatus(student, BorrowStatus.ACTIVE);
        Optional<Borrowing> overdue = borrowingRepository.findFirstOverdue(student, today);

        return new BorrowEligibilityView(
                activeCount,
                BORROW_LIMIT,
                overdue.isPresent(),
                overdue.map(b -> b.getBook().getTitle()).orElse(null),
                overdue.map(Borrowing::getDueDate).orElse(null));
    }

    /**
     * Resolves the domain Student for the authenticated principal.
     *
     * If this throws, the session references an account that is not a student
     * - a configuration fault, not a user error, so it is deliberately not a
     * BookWiseException.
     */
    private Student requireStudent(String username) {
        return studentRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal '" + username + "' is not a student account"));
    }
}
