package user;

public class Admin extends User {
    private static final long serialVersionUID = 1L;
    public Admin(String id, String username, String password, String role){
        super(id, username, password ,"ADMIN");
    }
}
