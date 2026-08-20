package lk.ac.nsbm.bookwise.repository;

import lk.ac.nsbm.bookwise.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for every account type. Used by AppUserDetailsService during
 * authentication, where the concrete subtype is not yet known.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
