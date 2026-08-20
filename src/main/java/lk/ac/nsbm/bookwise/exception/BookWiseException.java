package lk.ac.nsbm.bookwise.exception;

import org.springframework.http.HttpStatus;

/**
 * Root of every business failure BookWise can report.
 *
 * Design notes for the report:
 *
 *  - It extends RuntimeException on purpose. Spring's declarative transaction
 *    management rolls back automatically on unchecked exceptions only; a
 *    checked exception would commit the half-finished transaction unless every
 *    @Transactional were given rollbackFor. Making the whole hierarchy
 *    unchecked means rollback is the default and cannot be forgotten.
 *
 *  - Each subclass carries the data needed to explain itself
 *    ({@link #getUserMessage()}), so the presentation layer never has to
 *    reconstruct why the operation failed. That is what makes four genuinely
 *    distinct, specific messages possible instead of one generic error.
 *
 *  - Each subclass also names the HTTP status that fits it, so the same
 *    hierarchy serves the Thymeleaf pages and the REST API without duplication.
 */
public abstract class BookWiseException extends RuntimeException {

    protected BookWiseException(String technicalMessage) {
        super(technicalMessage);
    }

    /** Short heading for the error page, e.g. "Borrowing limit reached". */
    public abstract String getTitle();

    /** Full sentence written for the student, naming the specific problem. */
    public abstract String getUserMessage();

    /** What the student can actually do about it. */
    public abstract String getSuggestedAction();

    /** Stable machine-readable code for the REST API and for log searching. */
    public abstract String getErrorCode();

    public abstract HttpStatus getHttpStatus();
}
