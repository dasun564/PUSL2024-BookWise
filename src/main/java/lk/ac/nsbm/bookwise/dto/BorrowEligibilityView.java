package lk.ac.nsbm.bookwise.dto;

import java.time.LocalDate;

/**
 * A student's current standing against the personalised borrowing rule
 * (student 10965261, band D = 0-4: at most 3 concurrent loans, and none at all
 * while an overdue book is held).
 *
 * Computed by LibraryServiceImpl and shown as a banner on the catalogue page,
 * so the rule is visible to the student before they click Borrow rather than
 * only in the rejection message afterwards.
 */
public record BorrowEligibilityView(
        long activeCount,
        int limit,
        boolean hasOverdue,
        String overdueTitle,
        LocalDate overdueDueDate) {

    public boolean canBorrow() {
        return activeCount < limit && !hasOverdue;
    }

    public long remainingAllowance() {
        return Math.max(0, limit - activeCount);
    }
}
