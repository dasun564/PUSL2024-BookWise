package lk.ac.nsbm.bookwise.config;

import lk.ac.nsbm.bookwise.entity.Admin;
import lk.ac.nsbm.bookwise.entity.Book;
import lk.ac.nsbm.bookwise.entity.Borrowing;
import lk.ac.nsbm.bookwise.entity.EBook;
import lk.ac.nsbm.bookwise.entity.PrintedBook;
import lk.ac.nsbm.bookwise.entity.Student;
import lk.ac.nsbm.bookwise.repository.AppUserRepository;
import lk.ac.nsbm.bookwise.repository.BookRepository;
import lk.ac.nsbm.bookwise.repository.BorrowingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Seeds the test data the coursework evidence depends on.
 *
 * Runs once: if any account already exists the seeder does nothing, so
 * restarting the application never duplicates data or resets loans made
 * through the UI. (The database is H2 in FILE mode, so it persists.)
 *
 * THREE student accounts exist on purpose, one per screenshot that Part E
 * requires. The personalised rule for 10965261 is "at most 3 active loans AND
 * no overdue book held", and an overdue loan is still an ACTIVE loan. A single
 * account holding 3 loans plus an overdue one would therefore have 4 active
 * loans and would always fail the limit check first, making the overdue
 * rejection impossible to reach or photograph. The states are separated:
 *
 *   10965261   - no active loans          -> successful borrow, and the
 *                                            "no copies available" rejection
 *   10965261B  - exactly 3 active loans,  -> "borrowing limit reached"
 *                none overdue
 *   10965261C  - 1 active loan, overdue   -> "you have an overdue book"
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Matches LibraryServiceImpl.LOAN_DAYS for the 0-4 band. */
    private static final int LOAN_DAYS = 14;

    private final AppUserRepository appUserRepository;
    private final BookRepository bookRepository;
    private final BorrowingRepository borrowingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository appUserRepository,
                      BookRepository bookRepository,
                      BorrowingRepository borrowingRepository,
                      PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.bookRepository = bookRepository;
        this.borrowingRepository = borrowingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            log.info("Seed data already present - skipping DataSeeder");
            return;
        }

        log.info("Seeding BookWise demonstration data for student 10965261");

        // ---------------------------------------------------------------
        // Accounts
        // ---------------------------------------------------------------
        Student main = new Student("10965261",
                passwordEncoder.encode("student123"),
                "Dasun Edirisinghe", "10965261");

        Student atLimit = new Student("10965261B",
                passwordEncoder.encode("student123"),
                "Dasun Edirisinghe (limit test account)", "10965261B");

        Student withOverdue = new Student("10965261C",
                passwordEncoder.encode("student123"),
                "Dasun Edirisinghe (overdue test account)", "10965261C");

        Admin admin = new Admin("admin10965261",
                passwordEncoder.encode("admin123"),
                "Library Administrator", "LIB-10965261");

        appUserRepository.save(main);
        appUserRepository.save(atLimit);
        appUserRepository.save(withOverdue);
        appUserRepository.save(admin);

        // ---------------------------------------------------------------
        // Catalogue
        // ---------------------------------------------------------------
        Book handbook = save(new PrintedBook(
                "Software Engineering Handbook (10965261 Edition)", "D. Edirisinghe",
                "9780000109652", "Software Engineering", 5, 5, "SE-A-01", "New"));

        Book cleanCode = save(new PrintedBook(
                "Clean Code", "Robert C. Martin",
                "9780132350884", "Software Engineering", 3, 3, "SE-A-02", "Good"));

        Book designPatterns = save(new PrintedBook(
                "Design Patterns", "Erich Gamma",
                "9780201633610", "Software Engineering", 1, 1, "SE-A-03", "Fair"));

        Book refactoring = save(new PrintedBook(
                "Refactoring", "Martin Fowler",
                "9780134757599", "Software Engineering", 2, 2, "SE-A-04", "Good"));

        Book umlDistilled = save(new PrintedBook(
                "UML Distilled", "Martin Fowler",
                "9780321193681", "Systems Analysis", 2, 2, "SA-B-01", "Good"));

        save(new PrintedBook(
                "Introduction to Algorithms", "Thomas H. Cormen",
                "9780262046305", "Algorithms", 4, 4, "AL-C-01", "New"));

        save(new PrintedBook(
                "Database System Concepts", "Abraham Silberschatz",
                "9780078022159", "Databases", 3, 3, "DB-D-01", "Good"));

        save(new EBook(
                "Spring in Action", "Craig Walls",
                "9781617297571", "Web Development", 10, 10, 12.4, "/downloads/spring-in-action.pdf"));

        save(new EBook(
                "Effective Java", "Joshua Bloch",
                "9780134685991", "Programming Languages", 8, 8, 8.9, "/downloads/effective-java.pdf"));

        save(new EBook(
                "Head First Design Patterns", "Eric Freeman",
                "9781492078005", "Software Engineering", 6, 6, 21.7, "/downloads/hfdp.pdf"));

        save(new EBook(
                "Software Testing Foundations", "Andreas Spillner",
                "9781937538421", "Quality Assurance", 5, 5, 6.2, "/downloads/testing-foundations.pdf"));

        LocalDate today = LocalDate.now();

        // ---------------------------------------------------------------
        // 10965261B - exactly three active loans, all still within their due
        // dates. Produces the "borrowing limit reached (3/3)" rejection.
        // Borrowing the only copy of Design Patterns also drives its
        // availableCopies to 0, which is what produces the separate
        // "no copies available" rejection for the main account.
        // ---------------------------------------------------------------
        lend(atLimit, designPatterns, today.minusDays(3));
        lend(atLimit, cleanCode, today.minusDays(5));
        lend(atLimit, refactoring, today.minusDays(1));

        // ---------------------------------------------------------------
        // 10965261C - a single loan, taken out 30 days ago on a 14-day period,
        // so it is 16 days overdue. Produces the "overdue book held" rejection.
        // ---------------------------------------------------------------
        lend(withOverdue, umlDistilled, today.minusDays(30));

        // ---------------------------------------------------------------
        // 10965261 - no ACTIVE loans, so the happy path is clear. One
        // completed loan gives the My Borrowings page some history without
        // counting towards the limit.
        // ---------------------------------------------------------------
        Borrowing finished = lend(main, handbook, today.minusDays(40));
        finished.markReturned(today.minusDays(29));   // returned before its due date
        handbook.incrementCopies();

        log.info("Seed complete: {} accounts, {} books, {} borrowings",
                appUserRepository.count(), bookRepository.count(), borrowingRepository.count());
        log.info("Sign in as 10965261 / student123 (student) or admin10965261 / admin123 (staff)");
    }

    private Book save(Book book) {
        return bookRepository.save(book);
    }

    /**
     * Creates a loan exactly the way LibraryServiceImpl does - decrementing
     * stock and applying the same 14-day loan period - so the seeded rows are
     * consistent with rows created through the UI.
     */
    private Borrowing lend(Student student, Book book, LocalDate borrowDate) {
        book.decrementCopies();
        Borrowing borrowing = new Borrowing(student, book, borrowDate, borrowDate.plusDays(LOAN_DAYS));
        return borrowingRepository.save(borrowing);
    }
}
