package database.model;

/**
 * VehicleItem – Phương tiện (xe cộ).
 * Kế thừa Item. Áp dụng: Inheritance, Polymorphism.
 */
public class VehicleItem extends Item {

    private String brand;
    private int    year;
    private int    mileage;

    public VehicleItem(int id, String name, String description,
                       double startingPrice, double minStep, int duration,
                       String sellerName, String condition, String status,
                       String brand, int year, int mileage) {
        super(id, name, description, startingPrice, minStep,
                duration, sellerName, condition, status);
        this.brand   = brand;
        this.year    = year;
        this.mileage = mileage;
    }

    public String getBrand()  { return brand; }
    public int    getYear()   { return year; }
    public int    getMileage(){ return mileage; }

    @Override
    public String getCategoryName() { return "Xe cộ"; }

    @Override
    public String getInfo() {
        return super.getInfo() + " | " + brand + " " + year + " | " + mileage + " km";
    }
}
