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

/**
 * Abstract base for every account that can authenticate against BookWise.
 *
 * Generalisation: {@link Student} and {@link Admin} inherit identity and
 * credential handling from here, so the authentication code
 * ({@code AppUserDetailsService}) works against one type instead of two.
 *
 * SINGLE_TABLE is used because authentication always loads a user by username
 * without yet knowing which subtype it is; a single table makes that a single
 * indexed lookup with no join and no union.
 */
@Entity
@Table(name = "app_user")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    /** Always a BCrypt hash - never a plaintext password. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 120)
    private String fullName;

    protected AppUser() {
        // required by JPA
    }

    protected AppUser(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    /**
     * Spring Security authority for this account, without the {@code ROLE_}
     * prefix. Polymorphic: each subclass answers for itself, so adding a new
     * kind of user never requires editing the security code.
     */
    public abstract String getRole();

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
