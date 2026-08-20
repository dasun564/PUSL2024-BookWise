package lk.ac.nsbm.bookwise.controller;

import lk.ac.nsbm.bookwise.entity.Book;
import lk.ac.nsbm.bookwise.entity.BorrowStatus;
import lk.ac.nsbm.bookwise.entity.Student;
import lk.ac.nsbm.bookwise.repository.BookRepository;
import lk.ac.nsbm.bookwise.repository.BorrowingRepository;
import lk.ac.nsbm.bookwise.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Authentication, role-based authorisation, CSRF and - most importantly for
 * Part E - the fact that the borrowing student's identity is taken from the
 * authenticated session and not from anything the request carries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowingRepository borrowingRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @WithAnonymousUser
    @DisplayName("An anonymous visitor is sent to the login page")
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithUserDetails("10965261")
    @DisplayName("A student may not reach catalogue management")
    void studentCannotReachAdminArea() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin10965261")
    @DisplayName("An admin may reach catalogue management")
    void adminCanReachAdminArea() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/book-list"));
    }

    @Test
    @WithUserDetails("admin10965261")
    @DisplayName("An admin may not borrow - borrowing is a student action")
    void adminCannotBorrow() throws Exception {
        mockMvc.perform(post("/borrow/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("10965261")
    @DisplayName("A borrow request without a CSRF token is refused")
    void borrowWithoutCsrfIsRefused() throws Exception {
        mockMvc.perform(post("/borrow/1"))
                .andExpect(status().isForbidden());
    }

    /**
     * The security property Part E asks about.
     *
     * The request below carries a studentId parameter naming a DIFFERENT
     * account. The loan must still be recorded against the signed-in user,
     * because BorrowController reads the username from Authentication and the
     * controller has no parameter that could receive the forged value.
     *
     * Were the identity taken from the request, this is exactly how a student
     * would borrow books onto a classmate's account - an insecure direct
     * object reference, i.e. horizontal privilege escalation.
     */
    @Test
    @WithUserDetails("10965261")
    @DisplayName("A forged studentId in the request is ignored; the session owner is charged")
    void borrowerIdentityComesFromTheSessionNotTheRequest() throws Exception {
        Book target = bookRepository.findByActiveTrueOrderByTitleAsc().stream()
                .filter(b -> b.getAvailableCopies() > 0)
                .findFirst()
                .orElseThrow();

        int signedInBefore = activeLoanCount("10965261", target);
        int forgedBefore = activeLoanCount("10965261B", target);

        mockMvc.perform(post("/borrow/" + target.getId())
                        .param("studentId", "10965261B")
                        .param("username", "10965261B")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("borrow/confirm"));

        assertThat(activeLoanCount("10965261", target))
                .as("the signed-in student must have been charged for the loan")
                .isEqualTo(signedInBefore + 1);

        assertThat(activeLoanCount("10965261B", target))
                .as("the account named in the forged parameters must be untouched")
                .isEqualTo(forgedBefore);
    }

    /**
     * Active loans of one book held by one student. Uses the fetch-joining
     * query because open-in-view is disabled, so lazy associations cannot be
     * navigated once the repository call has returned.
     */
    private int activeLoanCount(String username, Book book) {
        Student student = studentRepository.findByUsername(username).orElseThrow();
        return (int) borrowingRepository.findAllByStudentWithBook(student).stream()
                .filter(b -> b.getStatus() == BorrowStatus.ACTIVE)
                .filter(b -> b.getBook().getId().equals(book.getId()))
                .count();
    }
}
