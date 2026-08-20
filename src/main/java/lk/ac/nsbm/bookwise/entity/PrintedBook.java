package lk.ac.nsbm.bookwise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Physical catalogue item. Adds the two attributes that only make sense for a
 * copy sitting on a shelf.
 */
@Entity
@DiscriminatorValue("PRINTED")
public class PrintedBook extends Book {

    @Column(name = "shelf_location", length = 40)
    private String shelfLocation;

    /** "condition" is a reserved word in SQL, so the column is renamed. */
    @Column(name = "book_condition", length = 30)
    private String bookCondition;

    protected PrintedBook() {
        // required by JPA
    }

    public PrintedBook(String title, String author, String isbn, String category,
                       int totalCopies, int availableCopies, String shelfLocation, String bookCondition) {
        super(title, author, isbn, category, totalCopies, availableCopies);
        this.shelfLocation = shelfLocation;
        this.bookCondition = bookCondition;
    }

    @Override
    public String getFormatLabel() {
        return "Printed Book";
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public String getBookCondition() {
        return bookCondition;
    }

    public void setBookCondition(String bookCondition) {
        this.bookCondition = bookCondition;
    }
}
