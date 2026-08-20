package lk.ac.nsbm.bookwise.repository;

import lk.ac.nsbm.bookwise.entity.Book;
import lk.ac.nsbm.bookwise.entity.BorrowStatus;
import lk.ac.nsbm.bookwise.entity.Borrowing;
import lk.ac.nsbm.bookwise.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access for loan records. The two queries the personalised borrowing
 * rule depends on live here - the rule itself does not, it lives in
 * LibraryServiceImpl. This layer answers questions about stored data; it never
 * decides what the answers mean.
 */
@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    /** How many books this student is holding right now. Feeds the 3-book limit. */
    long countByStudentAndStatus(Student student, BorrowStatus status);

    /**
     * The student's oldest unreturned loan whose due date has passed, if any.
     * Feeds the "no borrowing while holding an overdue book" half of the rule.
     */
    @Query("""
            select b from Borrowing b
            join fetch b.book
            where b.student = :student
              and b.status = lk.ac.nsbm.bookwise.entity.BorrowStatus.ACTIVE
              and b.dueDate < :today
            order by b.dueDate asc
            """)
    List<Borrowing> findOverdue(@Param("student") Student student, @Param("today") LocalDate today);

    default Optional<Borrowing> findFirstOverdue(Student student, LocalDate today) {
        return findOverdue(student, today).stream().findFirst();
    }

    /**
     * Loan history for the "My Borrowings" page. The book is fetch-joined
     * because open-in-view is disabled - the template cannot trigger a lazy
     * load once the transaction has ended.
     */
    @Query("""
            select b from Borrowing b
            join fetch b.book
            where b.student = :student
            order by case when b.status = lk.ac.nsbm.bookwise.entity.BorrowStatus.ACTIVE then 0 else 1 end,
                     b.dueDate asc
            """)
    List<Borrowing> findAllByStudentWithBook(@Param("student") Student student);

    @Query("""
            select b from Borrowing b
            join fetch b.book
            join fetch b.student
            where b.id = :id
            """)
    Optional<Borrowing> findByIdWithBookAndStudent(@Param("id") Long id);

    /** Used by the admin delete workflow to report how much history is at stake. */
    long countByBookAndStatus(Book book, BorrowStatus status);
}
