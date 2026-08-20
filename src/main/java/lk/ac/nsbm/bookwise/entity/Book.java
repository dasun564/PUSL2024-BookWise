package lk.ac.nsbm.bookwise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Abstract catalogue item. Concrete formats are {@link EBook} and
 * {@link PrintedBook}.
 *
 * JPA inheritance strategy: SINGLE_TABLE.
 * Justification for BookWise specifically - the dominant query in this system
 * is "search every book by title or category" across both formats at once.
 * With SINGLE_TABLE that is one indexed scan of one table with no join and no
 * union, so search stays fast and the polymorphic query is trivial to write.
 * The cost is that format-specific columns (shelfLocation, fileSizeMb) must be
 * nullable, so the database cannot enforce NOT NULL on them; that integrity
 * constraint has to move up into Bean Validation, which is weaker because it
 * only runs when the application writes the row.
 */
@Entity
@Table(name = "book")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "book_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false)
    private int totalCopies;

    /**
     * Never mutated by a setter. Stock only moves through
     * {@link #decrementCopies()} and {@link #incrementCopies()}, which enforce
     * the invariant 0 <= availableCopies <= totalCopies.
     */
    @Column(nullable = false)
    private int availableCopies;

    /**
     * Soft-delete flag. Admin "delete" clears this rather than removing the
     * row, so historical Borrowing records keep a valid foreign key.
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Optimistic lock counter. Combined with the pessimistic row lock taken in
     * BookRepository.findByIdForUpdate this closes the lost-update window when
     * two students borrow the last copy simultaneously.
     */
    @Version
    private Long version;

    protected Book() {
        // required by JPA
    }

    protected Book(String title, String author, String isbn, String category, int totalCopies, int availableCopies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    /**
     * Human-readable format name. Polymorphic: the catalogue templates call
     * this on a {@code Book} reference and each subclass answers for itself,
     * so the view never needs to test the concrete type.
     */
    public abstract String getFormatLabel();

    public boolean isAvailable() {
        return active && availableCopies > 0;
    }

    /**
     * Takes one copy off the shelf. Guards its own invariant rather than
     * trusting the caller - the count can never go negative.
     */
    public void decrementCopies() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No copies left to lend for book id " + id);
        }
        this.availableCopies--;
    }

    /** Puts one copy back. Cannot exceed the number the library owns. */
    public void incrementCopies() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies of book id " + id + " are already on the shelf");
        }
        this.availableCopies++;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public boolean isActive() {
        return active;
    }

    /** Used only by BookAdminService.softDeleteBook and restoreBook. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Admin stock correction. Keeps availableCopies consistent by applying the
     * same delta, and never lets it fall outside the valid range.
     */
    public void adjustTotalCopies(int newTotal) {
        int onLoan = this.totalCopies - this.availableCopies;
        if (newTotal < onLoan) {
            throw new IllegalStateException(
                    "Cannot reduce total copies to " + newTotal + ": " + onLoan + " are currently on loan");
        }
        this.totalCopies = newTotal;
        this.availableCopies = newTotal - onLoan;
    }
}
