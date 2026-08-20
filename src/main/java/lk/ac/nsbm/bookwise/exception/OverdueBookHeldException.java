package lk.ac.nsbm.bookwise.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Failure 3b of 3 required by Part E - the second half of the PERSONALISED
 * borrowing rule for student 10965261 (band D = 0-4):
 * a book cannot be borrowed while the student already holds an overdue book.
 */
public class OverdueBookHeldException extends BookWiseException {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final String overdueTitle;
    private final LocalDate dueDate;
    private final long daysOverdue;

    public OverdueBookHeldException(String overdueTitle, LocalDate dueDate, LocalDate today) {
        super("Student holds overdue book '" + overdueTitle + "' due " + dueDate);
        this.overdueTitle = overdueTitle;
        this.dueDate = dueDate;
        this.daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
    }

    public String getOverdueTitle() {
        return overdueTitle;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getDaysOverdue() {
        return daysOverdue;
    }

    @Override
    public String getTitle() {
        return "You have an overdue book";
    }

    @Override
    public String getUserMessage() {
        return "\"" + overdueTitle + "\" was due back on " + dueDate.format(DISPLAY)
                + " and is now " + daysOverdue + " " + (daysOverdue == 1 ? "day" : "days")
                + " overdue. Borrowing is suspended until it is returned.";
    }

    @Override
    public String getSuggestedAction() {
        return "Return \"" + overdueTitle + "\" from the \"My Borrowings\" page to lift the restriction immediately.";
    }

    @Override
    public String getErrorCode() {
        return "OVERDUE_BOOK_HELD";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
