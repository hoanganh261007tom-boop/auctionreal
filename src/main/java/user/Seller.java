package user;

/**
 * Seller – Người bán hàng.
 * Kế thừa User. Áp dụng: Inheritance, Polymorphism.
 */
public class Seller extends User {

    private static final long serialVersionUID = 1L;
    private int totalItemsListed;

    public Seller(int id, String username, String password) {
        super(id, username, password, "SELLER");
    }

    /** Constructor cũ – giữ tương thích */
    public Seller(String id, String username, String password, String role) {
        super(Integer.parseInt(id), username, password, "SELLER");
    }

    public int  getTotalItemsListed()  { return totalItemsListed; }
    public void incrementItemsListed() { this.totalItemsListed++; }

    @Override
    public String getPermissions() {
        return "Đăng sản phẩm, Sửa/Xóa sản phẩm, Xem kết quả đấu giá";
    }

    @Override
    public String getInfo() {
        return super.getInfo() + " | Đã đăng: " + totalItemsListed + " sản phẩm";
    }
}
