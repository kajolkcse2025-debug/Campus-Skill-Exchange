package service;

import model.Student;
import model.User;
import util.Validation;

import java.util.ArrayList;
import java.util.List;

/**
 * MODULE 3: User Management
 * Registers students and looks them up for the other modules to use.
 */
public class UserService {

    private List<User> users = new ArrayList<>();
    private int nextId = 1;

    public Student registerStudent(String name) {
        if (!Validation.isNonEmpty(name)) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        Student student = new Student(nextId++, name);
        users.add(student);
        return student;
    }

    // POLYMORPHISM: overloaded find methods
    public User findByName(String name) {
        for (User u : users) {
            if (u.getName().equalsIgnoreCase(name)) return u;
        }
        return null;
    }

    public User findById(int id) {
        for (User u : users) {
            if (u.getId() == id) return u;
        }
        return null;
    }

    public List<User> getAllUsers() {
        return users;
    }
}
