package lk.ac.nsbm.bookwise.service;

import lk.ac.nsbm.bookwise.dto.BookView;
import lk.ac.nsbm.bookwise.dto.BorrowResultView;
import lk.ac.nsbm.bookwise.entity.Book;
import lk.ac.nsbm.bookwise.exception.BookNotFoundException;
import lk.ac.nsbm.bookwise.exception.BorrowLimitExceededException;
import lk.ac.nsbm.bookwise.exception.NoCopiesAvailableException;
import lk.ac.nsbm.bookwise.exception.OverdueBookHeldException;
import lk.ac.nsbm.bookwise.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for the PERSONALISED borrowing rule of student 10965261
 * (last digit 1, band D = 0-4): at most 3 concurrent loans, and no borrowing
 * at all while an overdue book is held.
 *
 * These run against the accounts DataSeeder creates, so they exercise exactly
 * the states the coursework screenshots demonstrate.
 */
@SpringBootTest
@ActiveProfiles("test")
class BorrowingRuleTest {

    private static final String CLEAN_STUDENT = "10965261";     // no active loans
    private static final String AT_LIMIT_STUDENT = "10965261B"; // exactly 3 active loans
    private static final String OVERDUE_STUDENT = "10965261C";  // 1 loan, overdue

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private BookRepository bookRepository;

    // -----------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------

    @Test
    @DisplayName("A student under the limit borrows successfully and stock drops by one")
    void borrowSucceeds() {
        Book target = availableBook();
        int before = target.getAvailableCopies();

        BorrowResultView result = libraryService.borrowBook(CLEAN_STUDENT, target.getId());

        assertThat(result.bookTitle()).isEqualTo(target.getTitle());
        assertThat(result.studentUsername()).isEqualTo(CLEAN_STUDENT);
        assertThat(result.studentName()).isEqualTo("Dasun Edirisinghe");
        assertThat(result.copiesRemaining()).isEqualTo(before - 1);
        assertThat(result.dueDate()).isEqualTo(result.borrowDate().plusDays(14));
    }

    @Test
    @DisplayName("The loan period for band 0-4 is 14 days")
    void loanPeriodIsFourteenDays() {
        Book target = availableBook();
        BorrowResultView result = libraryService.borrowBook(CLEAN_STUDENT, target.getId());

        assertThat(result.loanDays()).isEqualTo(14);
        assertThat(result.borrowDate()).isEqualTo(LocalDate.now());
    }

    // -----------------------------------------------------------------
    // The three failure reasons Part E requires, each distinct
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Borrowing a book that does not exist is rejected as BOOK_NOT_FOUND")
    void rejectsUnknownBook() {
        assertThatExceptionOfType(BookNotFoundException.class)
                .isThrownBy(() -> libraryService.borrowBook(CLEAN_STUDENT, 999_999L))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("BOOK_NOT_FOUND");
                    assertThat(ex.getUserMessage()).contains("999999");
                });
    }

    @Test
    @DisplayName("Borrowing a fully loaned book is rejected as NO_COPIES_AVAILABLE")
    void rejectsWhenNoCopiesLeft() {
        Book exhausted = bookRepository.findByActiveTrueOrderByTitleAsc().stream()
                .filter(b -> b.getAvailableCopies() == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data should contain a fully loaned book"));

        assertThatExceptionOfType(NoCopiesAvailableException.class)
                .isThrownBy(() -> libraryService.borrowBook(CLEAN_STUDENT, exhausted.getId()))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("NO_COPIES_AVAILABLE");
                    assertThat(ex.getUserMessage()).contains(exhausted.getTitle());
                });
    }

    @Test
    @DisplayName("PERSONALISED RULE: a fourth concurrent loan is rejected")
    void rejectsFourthConcurrentLoan() {
        Book target = availableBook();

        assertThatExceptionOfType(BorrowLimitExceededException.class)
                .isThrownBy(() -> libraryService.borrowBook(AT_LIMIT_STUDENT, target.getId()))
                .satisfies(ex -> {
                    assertThat(ex.getLimit()).isEqualTo(3);
                    assertThat(ex.getCurrentCount()).isEqualTo(3);
                    assertThat(ex.getErrorCode()).isEqualTo("BORROW_LIMIT_EXCEEDED");
                });
    }

    @Test
    @DisplayName("PERSONALISED RULE: holding an overdue book blocks borrowing entirely")
    void rejectsWhileOverdueBookHeld() {
        Book target = availableBook();

        assertThatExceptionOfType(OverdueBookHeldException.class)
                .isThrownBy(() -> libraryService.borrowBook(OVERDUE_STUDENT, target.getId()))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo("OVERDUE_BOOK_HELD");
                    assertThat(ex.getDaysOverdue()).isPositive();
                    assertThat(ex.getDueDate()).isBefore(LocalDate.now());
                });
    }

    // -----------------------------------------------------------------
    // Regression test for the defect found while drawing the Part D
    // activity diagram: availability must be evaluated before the
    // personalised rule, so the student is told the real reason.
    // -----------------------------------------------------------------

    @Test
    @DisplayName("A student at the limit clicking a fully loaned book is told about the copies, not the limit")
    void availabilityIsCheckedBeforeTheBorrowLimit() {
        Book exhausted = bookRepository.findByActiveTrueOrderByTitleAsc().stream()
                .filter(b -> b.getAvailableCopies() == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data should contain a fully loaned book"));

        assertThatExceptionOfType(NoCopiesAvailableException.class)
                .isThrownBy(() -> libraryService.borrowBook(AT_LIMIT_STUDENT, exhausted.getId()));
    }

    // -----------------------------------------------------------------
    // Atomicity: a rejected borrow must leave no trace
    // -----------------------------------------------------------------

    @Test
    @DisplayName("A rejected borrow rolls back: no copy is lost")
    void rejectedBorrowLeavesStockUntouched() {
        Book target = availableBook();
        Long id = target.getId();
        int before = target.getAvailableCopies();

        assertThatExceptionOfType(BorrowLimitExceededException.class)
                .isThrownBy(() -> libraryService.borrowBook(AT_LIMIT_STUDENT, id));

        int after = bookRepository.findById(id).orElseThrow().getAvailableCopies();
        assertThat(after)
                .as("availableCopies must be unchanged after a rejected borrow")
                .isEqualTo(before);
    }

    // -----------------------------------------------------------------
    // Search
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Search matches on title and on category")
    void searchMatchesTitleAndCategory() {
        assertThat(libraryService.searchBooks("clean"))
                .extracting(BookView::title)
                .anyMatch(t -> t.contains("Clean Code"));

        assertThat(libraryService.searchBooks("Databases"))
                .isNotEmpty()
                .allSatisfy(b -> assertThat(b.category()).isEqualTo("Databases"));

        assertThatNoException().isThrownBy(() -> libraryService.searchBooks(null));
    }

    /** Any active title that still has a copy on the shelf. */
    private Book availableBook() {
        return bookRepository.findByActiveTrueOrderByTitleAsc().stream()
                .filter(b -> b.getAvailableCopies() > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data should contain an available book"));
    }
}
