# BookWise — University Library Borrowing System

PUSL2024 Software Engineering 2 — Referral Coursework 2025–2026
**Dasun Edirisinghe — 10965261**

A Spring Boot web application that lets library staff manage a book catalogue and
lets students search, borrow and return books.

---

## Personalised borrowing rule

The last digit of student ID **10965261** is **1**, which places this submission in
band **D = 0–4**:

> A student may borrow a maximum of **3 books at a time**, and a book **cannot be
> borrowed if the student already has an overdue book**.

The 0–4 band does not state a loan period, so **14 days** is adopted for every
format and declared as a stated assumption.

The rule is enforced in exactly one place:
[`LibraryServiceImpl.borrowBook`](src/main/java/lk/ac/nsbm/bookwise/service/LibraryServiceImpl.java).

---

## Running it

Requires **JDK 17 or later** (built and tested on JDK 21). Maven is *not* required —
the Maven wrapper downloads it.

```bash
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

Then open <http://localhost:8080>.

To run the tests:

```bash
./mvnw test
```

### Demonstration accounts

Created automatically on first start by
[`DataSeeder`](src/main/java/lk/ac/nsbm/bookwise/config/DataSeeder.java).

| Username | Password | State | Demonstrates |
|---|---|---|---|
| `10965261` | `student123` | no active loans | successful borrow; "no copies" rejection |
| `10965261B` | `student123` | exactly 3 active loans | "borrowing limit reached" rejection |
| `10965261C` | `student123` | 1 loan, 16 days overdue | "overdue book held" rejection |
| `admin10965261` | `admin123` | library staff | catalogue CRUD, soft delete |

Three student accounts exist because an overdue loan is still an *active* loan. One
account holding three loans plus an overdue one would have four active loans and
would always fail the limit check first, making the overdue rejection unreachable.

### Database

H2 in **file mode** at `./data/bookwise.mv.db`, so data survives a restart.

Console: <http://localhost:8080/h2-console>
JDBC URL: `jdbc:h2:file:./data/bookwise` — user `sa`, no password.

Deleting the `data/` directory resets everything; the seeder repopulates on next start.

---

## Architecture

Strict layering, enforced by having no downward shortcuts:

```
Controller   HTTP translation and view selection. No business rules,
   ↓         no repository calls, no try/catch.
Service      Transaction boundary and all business rules, including the
   ↓         personalised borrowing rule.
Repository   Spring Data JPA query methods. No decisions.
   ↓
Entity       Persistent state, guarding its own invariants.
```

| Package | Contents |
|---|---|
| `entity` | `Book` (abstract, `SINGLE_TABLE`) → `EBook`, `PrintedBook`; `AppUser` (abstract) → `Student`, `Admin`; `Borrowing`, `BorrowStatus` |
| `repository` | `BookRepository` (incl. `findByIdForUpdate` with a pessimistic write lock), `BorrowingRepository`, `StudentRepository`, `AppUserRepository` |
| `service` | `LibraryService` / `LibraryServiceImpl`, `BookAdminService`, `AppUserDetailsService` |
| `controller` | `BookController`, `BorrowController`, `AdminBookController`, `BookRestController`, `AuthController`, `CurrentUserAdvice` |
| `dto` | `BookForm` (validated input), `BookView`, `BorrowResultView`, `BorrowingView`, `BorrowEligibilityView` (read-only records) |
| `exception` | `BookWiseException` hierarchy + `GlobalExceptionHandler`, `RestExceptionHandler` |
| `config` | `SecurityConfig`, `DataSeeder` |

### Key design decisions

**Atomicity.** `borrowBook` is `@Transactional`. Spring wraps the bean in an AOP proxy
that commits on normal return and rolls back on any unchecked exception. Every
`BookWiseException` is unchecked, so a rejection undoes the copy decrement. Without
it, a failure after the decrement would leave a book with a copy subtracted and no
borrowing record — stock lost permanently.

**Concurrency.** `findByIdForUpdate` takes a `PESSIMISTIC_WRITE` lock, and `Book`
carries a `@Version` column. Two students clicking Borrow on the last copy cannot
both succeed: the second transaction blocks on the row lock until the first commits,
then re-reads `availableCopies` as 0 and is rejected.

**Identity.** The borrow form posts only the book id. The student is resolved from
`Authentication.getName()` — the server-side session. `SecurityAccessTest` proves a
forged `studentId` parameter is ignored. Trusting a submitted id would be an insecure
direct object reference, letting any student borrow onto a classmate's account.

**Error handling.** Centralised in `@ControllerAdvice` / `@RestControllerAdvice`, never
in per-method try/catch. Each exception carries its own data and produces its own
specific message — four distinct rejections, no generic "an error occurred".

**Deletion.** Admin delete is a **soft delete** (`active = false`). Borrowing rows
reference `book_id`; a hard delete would either violate that constraint or, with a
cascade, erase the loan history a library needs to keep.

---

## Endpoints

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/login` | public | Sign-in page |
| GET | `/books` | authenticated | Catalogue, `?q=` searches title / author / category |
| GET | `/books/{id}` | authenticated | Book details |
| POST | `/borrow/{bookId}` | STUDENT | Borrow — identity from session |
| POST | `/return/{borrowingId}` | STUDENT | Return |
| GET | `/my-books` | STUDENT | Loan history |
| GET | `/admin/books` | ADMIN | Manage catalogue |
| GET/POST | `/admin/books/new`, `/admin/books` | ADMIN | Create, with Bean Validation |
| GET/POST | `/admin/books/{id}/edit`, `/admin/books/{id}` | ADMIN | Update |
| POST | `/admin/books/{id}/delete` | ADMIN | Soft delete |
| GET | `/api/books`, `/api/books/{id}` | authenticated | REST read |
| POST/PUT/DELETE | `/api/books`, `/api/books/{id}` | ADMIN | REST write |
| GET | `/h2-console` | public | Database console (evidence only) |

---

## Failure messages

| Error code | Shown when | Message |
|---|---|---|
| `BOOK_NOT_FOUND` | no such active book | "We could not find a book with reference number *n*…" |
| `NO_COPIES_AVAILABLE` | every copy on loan | "All *n* copies of *"title"* are currently on loan…" |
| `BORROW_LIMIT_EXCEEDED` | **personalised rule** | "You already have 3 books on loan and your borrowing limit is 3…" |
| `OVERDUE_BOOK_HELD` | **personalised rule** | "*"title"* was due back on *date* and is now *n* days overdue…" |

---

## Tests

16 tests, all passing:

- `BorrowingRuleTest` — the 3-book limit, the overdue block, both catalogue failures,
  the 14-day loan period, rollback on rejection, and a regression test that
  availability is evaluated before the personalised rule.
- `SecurityAccessTest` — anonymous redirect, student blocked from `/admin`, admin
  blocked from borrowing, CSRF enforcement, and session-derived borrower identity.
- `BookwiseApplicationTests` — full context loads.
