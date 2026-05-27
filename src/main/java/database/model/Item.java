package database.model;

/**
 * Item – Abstract class đại diện cho sản phẩm đấu giá.
 * Kế thừa Entity. Các subclass: ElectronicsItem, ArtItem, VehicleItem, GeneralItem
 * Áp dụng OOP: Inheritance, Abstraction, Encapsulation, Polymorphism
 */
public abstract class Item extends Entity {

    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private double minStep;
    private int    duration;
    private String sellerName;
    private String condition;
    private String status;

    public Item(int id, String name, String description,
                double startingPrice, double minStep,
                int duration, String sellerName,
                String condition, String status) {
        super(id);
        this.name          = name;
        this.description   = description;
        this.startingPrice = startingPrice;
        this.currentPrice  = startingPrice;
        this.minStep       = minStep;
        this.duration      = duration;
        this.sellerName    = sellerName;
        this.condition     = condition;
        this.status        = status;
    }

    // ── Getters & Setters – Encapsulation ──
    public String getName()                  { return name; }
    public String getDescription()           { return description; }
    public double getStartingPrice()         { return startingPrice; }
    public double getCurrentPrice()          { return currentPrice; }
    public void   setCurrentPrice(double p)  { this.currentPrice = p; }
    public double getMinStep()               { return minStep; }
    public int    getDuration()              { return duration; }
    public String getSellerName()            { return sellerName; }
    public String getCondition()             { return condition; }
    public String getStatus()                { return status; }
    public void   setStatus(String s)        { this.status = s; }

    // Giữ tương thích với code cũ
    public double getPrice()    { return currentPrice; }
    public String getSeller()   { return sellerName; }
    public String getCategory() { return getCategoryName(); }

    /**
     * Mỗi loại item trả về tên category riêng – Polymorphism
     * Subclass BẮT BUỘC override method này
     */
    public abstract String getCategoryName();

    @Override
    public String getInfo() {
        return String.format("[%s] %s | Giá: %.0f₫ | Bước: %.0f₫ | %s",
                getCategoryName(), name, currentPrice, minStep, status);
    }
}
