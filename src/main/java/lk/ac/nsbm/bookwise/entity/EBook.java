package lk.ac.nsbm.bookwise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Digital catalogue item. Adds the two attributes that only make sense for a
 * downloadable copy.
 */
@Entity
@DiscriminatorValue("EBOOK")
public class EBook extends Book {

    @Column(name = "file_size_mb")
    private Double fileSizeMb;

    @Column(name = "download_url", length = 300)
    private String downloadUrl;

    protected EBook() {
        // required by JPA
    }

    public EBook(String title, String author, String isbn, String category,
                 int totalCopies, int availableCopies, Double fileSizeMb, String downloadUrl) {
        super(title, author, isbn, category, totalCopies, availableCopies);
        this.fileSizeMb = fileSizeMb;
        this.downloadUrl = downloadUrl;
    }

    @Override
    public String getFormatLabel() {
        return "E-Book";
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
}
