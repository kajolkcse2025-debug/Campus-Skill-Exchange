package model;

// INHERITANCE: Student "is-a" User
public class Student extends User {

    public Student(int id, String name) {
        super(id, name); // calls User's constructor
    }

    @Override
    public String getRole() {
        return "Student";
    }
}
