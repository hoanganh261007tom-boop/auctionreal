package database.model;

/**
 * ElectronicsItem – Sản phẩm điện tử.
 * Kế thừa Item. Áp dụng: Inheritance, Polymorphism.
 */
public class ElectronicsItem extends Item {

    private String brand;
    private String warranty;

    public ElectronicsItem(int id, String name, String description,
                           double startingPrice, double minStep, int duration,
                           String sellerName, String condition, String status,
                           String brand, String warranty) {
        super(id, name, description, startingPrice, minStep,
                duration, sellerName, condition, status);
        this.brand   = brand;
        this.warranty = warranty;
    }

    public String getBrand()    { return brand; }
    public String getWarranty() { return warranty; }

    @Override
    public String getCategoryName() { return "Điện tử"; }

    @Override
    public String getInfo() {
        return super.getInfo() + " | Hãng: " + brand + " | BH: " + warranty;
    }
}
