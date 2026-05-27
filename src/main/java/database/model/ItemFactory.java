package database.model;

/**
 * ItemFactory – Factory Method Pattern.
 *
 * Tạo các loại Item khác nhau dựa vào category.
 * Che giấu logic khởi tạo object – client chỉ cần gọi createItem().
 *
 * Áp dụng Design Pattern: Factory Method
 *
 * Cách dùng:
 *   Item item = ItemFactory.createItem("Điện tử", id, name, desc, price, step, dur, seller, cond, status);
 */
public class ItemFactory {

    /**
     * Tạo Item phù hợp dựa vào category.
     * @param category "Điện tử" | "Nghệ thuật" | "Xe cộ" | anything else → GeneralItem
     */
    public static Item createItem(
            String category,
            int    id,
            String name,
            String description,
            double startingPrice,
            double minStep,
            int    duration,
            String sellerName,
            String condition,
            String status
    ) {
        if (category == null) category = "Khác";

        return switch (category.trim()) {
            case "Điện tử"    -> new ElectronicsItem(id, name, description,
                    startingPrice, minStep, duration,
                    sellerName, condition, status,
                    "Unknown", "Không có BH");

            case "Nghệ thuật" -> new ArtItem(id, name, description,
                    startingPrice, minStep, duration,
                    sellerName, condition, status,
                    "Unknown", 2024);

            case "Xe cộ"      -> new VehicleItem(id, name, description,
                    startingPrice, minStep, duration,
                    sellerName, condition, status,
                    "Unknown", 2024, 0);

            default           -> new GeneralItem(id, name, description,
                    startingPrice, minStep, duration,
                    sellerName, condition, status, category);
        };
    }

    /**
     * Overload tiện lợi: tạo ElectronicsItem với đầy đủ thông tin
     */
    public static ElectronicsItem createElectronics(
            int id, String name, String description,
            double startingPrice, double minStep, int duration,
            String sellerName, String condition, String status,
            String brand, String warranty) {
        return new ElectronicsItem(id, name, description, startingPrice, minStep,
                duration, sellerName, condition, status, brand, warranty);
    }

    /**
     * Overload tiện lợi: tạo ArtItem với đầy đủ thông tin
     */
    public static ArtItem createArt(
            int id, String name, String description,
            double startingPrice, double minStep, int duration,
            String sellerName, String condition, String status,
            String artist, int year) {
        return new ArtItem(id, name, description, startingPrice, minStep,
                duration, sellerName, condition, status, artist, year);
    }

    /**
     * Overload tiện lợi: tạo VehicleItem với đầy đủ thông tin
     */
    public static VehicleItem createVehicle(
            int id, String name, String description,
            double startingPrice, double minStep, int duration,
            String sellerName, String condition, String status,
            String brand, int year, int mileage) {
        return new VehicleItem(id, name, description, startingPrice, minStep,
                duration, sellerName, condition, status, brand, year, mileage);
    }
}
