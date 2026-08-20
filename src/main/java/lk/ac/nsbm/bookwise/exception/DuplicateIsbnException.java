package lk.ac.nsbm.bookwise.exception;

import org.springframework.http.HttpStatus;

/**
 * Catalogue integrity failure: an admin tried to add a book with an ISBN that
 * is already in use by another active title.
 *
 * The check lives in BookAdminService rather than in the controller, because
 * "an ISBN identifies exactly one catalogue entry" is a business rule and must
 * hold for the REST endpoint as well as the web form.
 */
public class DuplicateIsbnException extends BookWiseException {

    private final String isbn;

    public DuplicateIsbnException(String isbn) {
        super("ISBN already in use: " + isbn);
        this.isbn = isbn;
    }

    public String getIsbn() {
        return isbn;
    }

    @Override
    public String getTitle() {
        return "Duplicate ISBN";
    }

    @Override
    public String getUserMessage() {
        return "ISBN " + isbn + " is already registered to another book in the catalogue.";
    }

    @Override
    public String getSuggestedAction() {
        return "Check the ISBN, or edit the existing catalogue entry instead of creating a second one.";
    }

    @Override
    public String getErrorCode() {
        return "DUPLICATE_ISBN";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
