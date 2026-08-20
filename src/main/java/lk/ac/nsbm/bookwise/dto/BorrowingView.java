package lk.ac.nsbm.bookwise.dto;

import lk.ac.nsbm.bookwise.entity.BorrowStatus;
import lk.ac.nsbm.bookwise.entity.Borrowing;

import java.time.LocalDate;

/**
 * Read-only row for the "My Borrowings" table. Immutable record, so the view
 * cannot mutate loan state.
 */
public record BorrowingView(
        Long id,
        String bookTitle,
        String bookAuthor,
        String bookFormat,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        BorrowStatus status,
        boolean overdue,
        long daysRemaining) {

    public static BorrowingView from(Borrowing borrowing, LocalDate today) {
        return new BorrowingView(
                borrowing.getId(),
                borrowing.getBook().getTitle(),
                borrowing.getBook().getAuthor(),
                borrowing.getBook().getFormatLabel(),
                borrowing.getBorrowDate(),
                borrowing.getDueDate(),
                borrowing.getReturnDate(),
                borrowing.getStatus(),
                borrowing.isOverdue(today),
                borrowing.daysRemaining(today));
    }

    public boolean isActive() {
        return status == BorrowStatus.ACTIVE;
    }
}
