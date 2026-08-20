package lk.ac.nsbm.bookwise.entity;

/**
 * Lifecycle of a single {@link Borrowing}.
 *
 * ACTIVE       - the book is currently with the student. A loan whose dueDate
 *                has passed is still ACTIVE; "overdue" is ACTIVE + past due,
 *                which is what the personalised borrowing rule tests for.
 * RETURNED     - handed back on or before the due date.
 * RETURNED_LATE- handed back after the due date. Kept distinct so the history
 *                shows the student's record honestly.
 */
public enum BorrowStatus {
    ACTIVE,
    RETURNED,
    RETURNED_LATE
}
