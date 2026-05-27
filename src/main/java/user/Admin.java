package user;

/**
 * Admin – Quản trị viên hệ thống.
 * Kế thừa User. Áp dụng: Inheritance, Polymorphism.
 */
public class Admin extends User {

    private static final long serialVersionUID = 1L;

    public Admin(int id, String username, String password) {
        super(id, username, password, "ADMIN");
    }

    /** Constructor cũ – giữ tương thích */
    public Admin(String id, String username, String password, String role) {
        super(Integer.parseInt(id), username, password, "ADMIN");
    }

    @Override
    public String getPermissions() {
        return "Toàn quyền: Quản lý người dùng, Xóa phiên đấu giá, Xem thống kê hệ thống";
    }

    @Override
    public String getInfo() {
        return super.getInfo() + " | [FULL ACCESS]";
    }
}
