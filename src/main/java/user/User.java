package user;

import database.model.Entity;
import java.io.Serializable;

/**
 * User – Abstract class đại diện người dùng hệ thống.
 * Kế thừa Entity. Subclass: Bidder, Seller, Admin
 * Áp dụng OOP: Inheritance, Abstraction, Encapsulation, Polymorphism
 */
public abstract class User extends Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String role;

    public User(int id, String username, String password, String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    // ── Getters – Encapsulation ──
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }

    /**
     * Giữ tương thích với code cũ (getId() trả String)
     */

    /**
     * Mỗi loại user mô tả quyền hạn khác nhau – Polymorphism
     * Subclass BẮT BUỘC override
     */
    public abstract String getPermissions();

    @Override
    public String getInfo() {
        return "[" + role + "] " + username + " (ID: " + super.getId() + ")";
    }

    @Override
    public String toString() { return getInfo(); }
}

