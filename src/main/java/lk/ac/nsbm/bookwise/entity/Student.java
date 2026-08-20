package lk.ac.nsbm.bookwise.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A library member who can search, borrow and return books.
 *
 * Aggregation: a Student aggregates their {@link Borrowing} records. The
 * borrowings belong to the student's history, but a Borrowing also references
 * a Book that outlives the student's account, so this is aggregation rather
 * than composition.
 */
@Entity
@DiscriminatorValue("STUDENT")
public class Student extends AppUser {

    /** University registration number, e.g. 10965261. */
    @Column(length = 20)
    private String studentNumber;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Borrowing> borrowings = new ArrayList<>();

    protected Student() {
        // required by JPA
    }

    public Student(String username, String password, String fullName, String studentNumber) {
        super(username, password, fullName);
        this.studentNumber = studentNumber;
    }

    @Override
    public String getRole() {
        return "STUDENT";
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    /**
     * Unmodifiable view: callers may read the borrowing history but may not
     * add to or remove from it directly. Borrowings are only ever created by
     * {@code LibraryServiceImpl} inside a transaction.
     */
    public List<Borrowing> getBorrowings() {
        return Collections.unmodifiableList(borrowings);
    }

    /** Package-private hook used only when seeding the in-memory object graph. */
    void addBorrowing(Borrowing borrowing) {
        this.borrowings.add(borrowing);
    }
}
