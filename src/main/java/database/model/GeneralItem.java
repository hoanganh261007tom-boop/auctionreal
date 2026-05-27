package database.model;

/**
 * GeneralItem – Sản phẩm thông thường (dùng khi không rõ loại).
 * Kế thừa Item.
 */
public class GeneralItem extends Item {

    private String category;

    public GeneralItem(int id, String name, String description,
                       double startingPrice, double minStep, int duration,
                       String sellerName, String condition, String status,
                       String category) {
        super(id, name, description, startingPrice, minStep,
                duration, sellerName, condition, status);
        this.category = (category != null && !category.isBlank()) ? category : "Khác";
    }

    @Override
    public String getCategoryName() { return category; }
}
