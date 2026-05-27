package database.model;

/**
 * ArtItem – Tác phẩm nghệ thuật.
 * Kế thừa Item. Áp dụng: Inheritance, Polymorphism.
 */
public class ArtItem extends Item {

    private String artist;
    private int    yearCreated;

    public ArtItem(int id, String name, String description,
                   double startingPrice, double minStep, int duration,
                   String sellerName, String condition, String status,
                   String artist, int yearCreated) {
        super(id, name, description, startingPrice, minStep,
                duration, sellerName, condition, status);
        this.artist      = artist;
        this.yearCreated = yearCreated;
    }

    public String getArtist()     { return artist; }
    public int    getYearCreated(){ return yearCreated; }

    @Override
    public String getCategoryName() { return "Nghệ thuật"; }

    @Override
    public String getInfo() {
        return super.getInfo() + " | Nghệ sĩ: " + artist + " (" + yearCreated + ")";
    }
}
