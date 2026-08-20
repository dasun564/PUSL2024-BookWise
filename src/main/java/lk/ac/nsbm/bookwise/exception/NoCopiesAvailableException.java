package lk.ac.nsbm.bookwise.exception;

import org.springframework.http.HttpStatus;

/**
 * Failure 2 of 3 required by Part E: the book exists but every copy the
 * library owns is currently on loan to someone else.
 */
public class NoCopiesAvailableException extends BookWiseException {

    private final String bookTitle;
    private final int totalCopies;

    public NoCopiesAvailableException(String bookTitle, int totalCopies) {
        super("All " + totalCopies + " copies of '" + bookTitle + "' are on loan");
        this.bookTitle = bookTitle;
        this.totalCopies = totalCopies;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    @Override
    public String getTitle() {
        return "No copies available";
    }

    @Override
    public String getUserMessage() {
        return "All " + totalCopies + " " + (totalCopies == 1 ? "copy" : "copies")
                + " of \"" + bookTitle + "\" are currently on loan to other students.";
    }

    @Override
    public String getSuggestedAction() {
        return "Please check back after the current loans are returned, or browse a similar title in the same category.";
    }

    @Override
    public String getErrorCode() {
        return "NO_COPIES_AVAILABLE";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
