package lk.ac.nsbm.bookwise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * The permanent record of one student holding one book.
 *
 * ENCAPSULATION - this is the entity whose state a controller must never be
 * able to touch directly. Three deliberate choices enforce that:
 *
 *  1. {@code status} and {@code dueDate} have getters but NO setters, so there
 *     is no way to write {@code borrowing.setStatus(RETURNED)} anywhere.
 *  2. The only state transition is {@link #markReturned(LocalDate)}, whose
 *     signature does not accept a status at all. A caller cannot express
 *     "set the status to RETURNED"; it can only report "this came back on
 *     date X" and the object decides for itself whether that was late. The
 *     rule that late returns are recorded as late therefore cannot be
 *     bypassed, forgotten or contradicted by a caller.
 *  3. The method refuses to run twice, so a double-submitted return form
 *     cannot corrupt the record.
 *
 * Controllers additionally never receive this entity - they are handed the
 * read-only {@code BorrowResultView} / {@code BorrowingView} DTOs.
 */
@Entity
@Table(name = "borrowing")
public class Borrowing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private LocalDate borrowDate;

    /** No setter - fixed at construction from the loan period. */
    @Column(nullable = false)
    private LocalDate dueDate;

    /** No setter - written only by markReturned. */
    private LocalDate returnDate;

    /** No setter - written only by markReturned. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowStatus status = BorrowStatus.ACTIVE;

    protected Borrowing() {
        // required by JPA
    }

    public Borrowing(Student student, Book book, LocalDate borrowDate, LocalDate dueDate) {
        this.student = student;
        this.book = book;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = BorrowStatus.ACTIVE;
    }

    /**
     * The single permitted state transition.
     *
     * The caller supplies only the date the book came back. Whether that
     * counts as RETURNED or RETURNED_LATE is decided here, by comparing
     * against this loan's own due date - the invariant lives with the data it
     * governs.
     */
    public void markReturned(LocalDate on) {
        if (this.status != BorrowStatus.ACTIVE) {
            throw new IllegalStateException("Borrowing " + id + " has already been returned");
        }
        this.returnDate = on;
        this.status = on.isAfter(this.dueDate) ? BorrowStatus.RETURNED_LATE : BorrowStatus.RETURNED;
    }

    /**
     * A loan is overdue when it is still out and the due date has passed.
     * This is the exact condition the personalised borrowing rule blocks on.
     */
    public boolean isOverdue(LocalDate today) {
        return status == BorrowStatus.ACTIVE && today.isAfter(dueDate);
    }

    /** Negative once the book is late. Used for display only. */
    public long daysRemaining(LocalDate today) {
        return java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Book getBook() {
        return book;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public BorrowStatus getStatus() {
        return status;
    }
}
