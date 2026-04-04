package user;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private double balance;
    public Bidder(String id, String username, String password, double initialBalance){
        super(id, username, password, "BIDDER");
        this.balance = initialBalance;
    }
    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance) {
        this.balance = balance;
    }
}
