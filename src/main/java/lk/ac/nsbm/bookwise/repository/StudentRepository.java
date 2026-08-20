package lk.ac.nsbm.bookwise.repository;

import lk.ac.nsbm.bookwise.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for student accounts.
 *
 * The borrow workflow resolves the student from the username held in the
 * Spring Security session - never from anything the browser submitted.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUsername(String username);
}
