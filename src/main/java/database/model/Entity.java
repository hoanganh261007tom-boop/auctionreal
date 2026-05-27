package database.model;

/**
 * Entity – Abstract base class cho toàn bộ hệ thống.
 * Đây là gốc của cây kế thừa: Entity → Item → (Electronics/Art/Vehicle/General)
 *                                       → User  → (Bidder/Seller/Admin)
 * Áp dụng OOP: Abstraction, Encapsulation
 */
public abstract class Entity {

    private int id;

    public Entity(int id) {
        this.id = id;
    }

    public int getId()         { return id; }
    public void setId(int id)   { this.id = id; }

    /** Mỗi entity tự mô tả thông tin của mình – Polymorphism */
    public abstract String getInfo();

    @Override
    public String toString() { return getInfo(); }
}
