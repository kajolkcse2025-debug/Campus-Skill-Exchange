package model;

// INHERITANCE: Admin also "is-a" User, but plays a different role
public class Admin extends User {

    public Admin(int id, String name) {
        super(id, name);
    }

    @Override
    public String getRole() {
        return "Admin";
    }
}
