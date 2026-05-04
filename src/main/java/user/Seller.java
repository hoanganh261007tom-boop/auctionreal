package user;

public class Seller extends User {
    private static final long serialVersionUID = 1L;
    public Seller(String id, String username, String password, String role){
        super(id, username, password, "Seller");

    }
}
