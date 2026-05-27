package user;

/**
 * Bidder – Người tham gia đấu giá.
 * Kế thừa User. Áp dụng: Inheritance, Polymorphism.
 */
public class Bidder extends User {

    private static final long serialVersionUID = 1L;
    private double balance;

    /** Constructor mới (dùng int id) */
    public Bidder(int id, String username, String password, double balance) {
        super(id, username, password, "BIDDER");
        this.balance = balance;
    }

    /** Constructor cũ (dùng String id) – giữ để tương thích */
    public Bidder(String id, String username, String password, double balance) {
        super(Integer.parseInt(id), username, password, "BIDDER");
        this.balance = balance;
    }

    public double getBalance()         { return balance; }
    public void   setBalance(double b) { this.balance = b; }
    public void   addBalance(double b) { this.balance += b; }

    @Override
    public String getPermissions() {
        return "Tham gia đấu giá, Xem danh sách phiên, Theo dõi watchlist";
    }

    @Override
    public String getInfo() {
        return super.getInfo() + " | Số dư: " + String.format("%.0f₫", balance);
    }
}
