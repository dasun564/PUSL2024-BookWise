package lk.ac.nsbm.bookwise.exception;

import org.springframework.http.HttpStatus;

/**
 * Failure 3a of 3 required by Part E - a violation of the PERSONALISED
 * borrowing rule for student 10965261.
 *
 * Last digit of the student ID is 1, so band D = 0-4 applies:
 * a student may hold at most 3 books at a time.
 */
public class BorrowLimitExceededException extends BookWiseException {

    private final long currentCount;
    private final int limit;

    public BorrowLimitExceededException(long currentCount, int limit) {
        super("Student already holds " + currentCount + " active borrowings, limit is " + limit);
        this.currentCount = currentCount;
        this.limit = limit;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String getTitle() {
        return "Borrowing limit reached";
    }

    @Override
    public String getUserMessage() {
        return "You already have " + currentCount + " books on loan and your borrowing limit is "
                + limit + ". You cannot borrow another book until one of them is returned.";
    }

    @Override
    public String getSuggestedAction() {
        return "Open \"My Borrowings\" and return a book you have finished with, then try again.";
    }

    @Override
    public String getErrorCode() {
        return "BORROW_LIMIT_EXCEEDED";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
