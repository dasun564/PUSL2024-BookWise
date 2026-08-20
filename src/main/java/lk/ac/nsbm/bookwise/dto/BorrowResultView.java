package lk.ac.nsbm.bookwise.dto;

import lk.ac.nsbm.bookwise.entity.Borrowing;

import java.time.LocalDate;

/**
 * Read-only projection handed to the borrow confirmation page.
 *
 * A Java record, so it is immutable by construction: the view layer physically
 * cannot alter loan state. This is the other half of the encapsulation answer
 * in Part B - the controller never touches the {@code Borrowing} entity, only
 * this flattened copy.
 *
 * It carries the student's own name because Part E requires the confirmation
 * page to show the book details together with the borrower's name.
 */
public record BorrowResultView(
        Long borrowingId,
        String bookTitle,
        String bookAuthor,
        String bookIsbn,
        String bookCategory,
        String bookFormat,
        int copiesRemaining,
        String studentName,
        String studentUsername,
        LocalDate borrowDate,
        LocalDate dueDate,
        int loanDays) {

    public static BorrowResultView from(Borrowing borrowing, int loanDays) {
        return new BorrowResultView(
                borrowing.getId(),
                borrowing.getBook().getTitle(),
                borrowing.getBook().getAuthor(),
                borrowing.getBook().getIsbn(),
                borrowing.getBook().getCategory(),
                borrowing.getBook().getFormatLabel(),
                borrowing.getBook().getAvailableCopies(),
                borrowing.getStudent().getFullName(),
                borrowing.getStudent().getUsername(),
                borrowing.getBorrowDate(),
                borrowing.getDueDate(),
                loanDays);
    }
}
