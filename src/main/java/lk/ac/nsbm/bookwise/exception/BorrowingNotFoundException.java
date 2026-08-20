package lk.ac.nsbm.bookwise.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a student tries to return a loan that does not exist, or that
 * belongs to a different student.
 *
 * Both cases deliberately produce the same message. Telling an attacker
 * "that loan exists but is not yours" would leak the existence of other
 * students' borrowing records.
 */
public class BorrowingNotFoundException extends BookWiseException {

    private final Long borrowingId;

    public BorrowingNotFoundException(Long borrowingId) {
        super("No borrowing with id " + borrowingId + " for the authenticated student");
        this.borrowingId = borrowingId;
    }

    public Long getBorrowingId() {
        return borrowingId;
    }

    @Override
    public String getTitle() {
        return "Loan record not found";
    }

    @Override
    public String getUserMessage() {
        return "We could not find loan reference " + borrowingId + " on your account.";
    }

    @Override
    public String getSuggestedAction() {
        return "Open \"My Borrowings\" to see the books currently on loan to you.";
    }

    @Override
    public String getErrorCode() {
        return "BORROWING_NOT_FOUND";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
