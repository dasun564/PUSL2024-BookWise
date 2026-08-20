package lk.ac.nsbm.bookwise.exception;

import org.springframework.http.HttpStatus;

/**
 * Failure 1 of 3 required by Part E: the requested book does not exist
 * (or has been withdrawn from the catalogue by a soft delete).
 */
public class BookNotFoundException extends BookWiseException {

    private final Long requestedId;

    public BookNotFoundException(Long requestedId) {
        super("No active book with id " + requestedId);
        this.requestedId = requestedId;
    }

    public Long getRequestedId() {
        return requestedId;
    }

    @Override
    public String getTitle() {
        return "Book not found";
    }

    @Override
    public String getUserMessage() {
        return "We could not find a book with reference number " + requestedId
                + ". It may have been withdrawn from the catalogue since you last looked.";
    }

    @Override
    public String getSuggestedAction() {
        return "Search the catalogue again to find the current entry for this title.";
    }

    @Override
    public String getErrorCode() {
        return "BOOK_NOT_FOUND";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
