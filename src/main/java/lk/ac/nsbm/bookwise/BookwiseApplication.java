package lk.ac.nsbm.bookwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BookWise - university library borrowing system.
 *
 * PUSL2024 Software Engineering 2, Referral Coursework 2025-2026.
 * Student: Dasun Edirisinghe (10965261).
 *
 * Personalised borrowing rule, derived from the last digit of the student ID
 * (1, therefore band D = 0-4): a student may hold at most 3 active borrowings,
 * and may not borrow anything at all while holding an overdue book.
 */
@SpringBootApplication
public class BookwiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookwiseApplication.class, args);
    }
}
