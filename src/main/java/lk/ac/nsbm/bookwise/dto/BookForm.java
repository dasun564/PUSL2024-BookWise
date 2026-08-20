package lk.ac.nsbm.bookwise.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for admin create/edit, validated with Bean Validation (JSR-380).
 *
 * A dedicated form object is used instead of binding straight onto the Book
 * entity. That keeps request data out of the persistence context until it has
 * been validated, and stops a crafted request from writing fields the form was
 * never meant to expose - notably {@code availableCopies}, {@code active} and
 * {@code version}, which have no counterpart here at all.
 */
public class BookForm {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 120, message = "Author must be 120 characters or fewer")
    private String author;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(97(8|9))?\\d{9}(\\d|X)$",
             message = "Enter a valid 10 or 13 digit ISBN, digits only")
    private String isbn;

    @NotBlank(message = "Category is required")
    @Size(max = 60, message = "Category must be 60 characters or fewer")
    private String category;

    @NotNull(message = "Number of copies is required")
    @Min(value = 1, message = "The library must hold at least 1 copy")
    private Integer totalCopies;

    /** EBOOK or PRINTED - selects which Book subclass is instantiated. */
    @NotBlank(message = "Choose a format")
    @Pattern(regexp = "EBOOK|PRINTED", message = "Format must be either EBOOK or PRINTED")
    private String format = "PRINTED";

    // --- format-specific, optional: only read when the matching format is chosen ---

    private Double fileSizeMb;

    @Size(max = 300, message = "Download URL must be 300 characters or fewer")
    private String downloadUrl;

    @Size(max = 40, message = "Shelf location must be 40 characters or fewer")
    private String shelfLocation;

    @Size(max = 30, message = "Condition must be 30 characters or fewer")
    private String bookCondition;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(Integer totalCopies) {
        this.totalCopies = totalCopies;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Double getFileSizeMb() {
        return fileSizeMb;
    }

    public void setFileSizeMb(Double fileSizeMb) {
        this.fileSizeMb = fileSizeMb;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public String getBookCondition() {
        return bookCondition;
    }

    public void setBookCondition(String bookCondition) {
        this.bookCondition = bookCondition;
    }
}
