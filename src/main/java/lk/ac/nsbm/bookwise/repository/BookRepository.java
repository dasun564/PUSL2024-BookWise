package lk.ac.nsbm.bookwise.repository;

import jakarta.persistence.LockModeType;
import lk.ac.nsbm.bookwise.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the catalogue. Contains queries only - no business rules.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Loads a book and holds a write lock on its row until the surrounding
     * transaction commits (SELECT ... FOR UPDATE).
     *
     * This is what makes the borrow workflow safe when two students click
     * "Borrow" on the last copy at the same moment: the second transaction
     * blocks at this line until the first has committed its decrement, so it
     * re-reads availableCopies as 0 and is correctly rejected, instead of both
     * reading 1 and both succeeding.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    /** Catalogue view for students - soft-deleted books are invisible. */
    Optional<Book> findByIdAndActiveTrue(Long id);

    List<Book> findByActiveTrueOrderByTitleAsc();

    @Query("""
            select b from Book b
            where b.active = true
              and (lower(b.title) like lower(concat('%', :term, '%'))
                   or lower(b.category) like lower(concat('%', :term, '%'))
                   or lower(b.author) like lower(concat('%', :term, '%')))
            order by b.title asc
            """)
    List<Book> searchByTitleOrCategory(@Param("term") String term);

    @Query("select distinct b.category from Book b where b.active = true order by b.category asc")
    List<String> findDistinctCategories();

    boolean existsByIsbnAndActiveTrue(String isbn);
}
