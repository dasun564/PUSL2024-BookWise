package lk.ac.nsbm.bookwise.dto;

import lk.ac.nsbm.bookwise.entity.Book;

/**
 * Read-only projection of a catalogue entry, used by the REST API so that
 * internal fields ({@code version}, {@code active}) and JPA proxies are never
 * serialised straight onto the wire.
 */
public record BookView(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        String format,
        int totalCopies,
        int availableCopies,
        boolean available) {

    public static BookView from(Book book) {
        return new BookView(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getCategory(),
                book.getFormatLabel(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.isAvailable());
    }
}
