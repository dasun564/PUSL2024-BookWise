package lk.ac.nsbm.bookwise.service;

import lk.ac.nsbm.bookwise.dto.BookView;
import lk.ac.nsbm.bookwise.dto.BorrowEligibilityView;
import lk.ac.nsbm.bookwise.dto.BorrowResultView;
import lk.ac.nsbm.bookwise.dto.BorrowingView;

import java.util.List;

/**
 * Student-facing business operations: searching the catalogue, borrowing and
 * returning.
 *
 * Controllers depend on this interface, never on the implementation, so the
 * web layer is coupled to a contract rather than to a class. Spring injects
 * the single {@code @Service} implementation by constructor.
 *
 * Every method takes the caller's USERNAME, not a student id. The username is
 * supplied by the controller from Spring Security's Authentication object, so
 * there is no signature in this interface through which a browser-supplied
 * identity could enter the business layer at all.
 */
public interface LibraryService {

    List<BookView> searchBooks(String term);

    List<BookView> listCatalogue();

    List<String> listCategories();

    BookView getBook(Long bookId);

    /**
     * Borrows one copy of {@code bookId} for the authenticated student.
     *
     * @param username the authenticated principal's name, from the session
     * @throws lk.ac.nsbm.bookwise.exception.BookNotFoundException          no such active book
     * @throws lk.ac.nsbm.bookwise.exception.NoCopiesAvailableException     every copy is on loan
     * @throws lk.ac.nsbm.bookwise.exception.BorrowLimitExceededException   personalised rule: 3-book limit
     * @throws lk.ac.nsbm.bookwise.exception.OverdueBookHeldException       personalised rule: overdue book held
     */
    BorrowResultView borrowBook(String username, Long bookId);

    BorrowingView returnBook(String username, Long borrowingId);

    List<BorrowingView> listMyBorrowings(String username);

    BorrowEligibilityView describeEligibility(String username);
}
