package lk.ac.nsbm.bookwise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Library staff account. Admins maintain the catalogue; they never borrow.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends AppUser {

    @Column(length = 20)
    private String staffCode;

    protected Admin() {
        // required by JPA
    }

    public Admin(String username, String password, String fullName, String staffCode) {
        super(username, password, fullName);
        this.staffCode = staffCode;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    public String getStaffCode() {
        return staffCode;
    }
}
