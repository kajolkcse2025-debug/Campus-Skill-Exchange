package menu;

import model.Skill;
import model.User;
import service.CreditService;
import service.SkillService;
import service.UserService;
import util.InsufficientCreditsException;

import java.util.List;
import java.util.Scanner;

public class Menu {

    private final Scanner sc = new Scanner(System.in);
    private final UserService userService = new UserService();
    private final SkillService skillService = new SkillService();
    private final CreditService creditService = new CreditService();
    private User currentUser;

    public void start() {
        System.out.println("=== Campus Skill Exchange & Credit System ===");
        registerOrLogin();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose option: ");
            switch (choice) {
                case 1 -> addSkill();
                case 2 -> searchSkill();
                case 3 -> updateSkill();
                case 4 -> deleteSkill();
                case 5 -> earnCredits();
                case 6 -> spendCredits();
                case 7 -> viewProfile();
                case 8 -> { running = false; System.out.println("Goodbye, " + currentUser.getName() + "!"); }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
        sc.close();
    }

    private void registerOrLogin() {
        System.out.print("Enter your name to register/continue: ");
        String name = sc.nextLine();
        User existing = userService.findByName(name);
        currentUser = (existing != null) ? existing : userService.registerStudent(name);
        System.out.println("Welcome, " + currentUser.getName() + "!");
    }

    private void printMenu() {
        System.out.println("\n--- Menu ---");
        System.out.println("1. Register Skill");
        System.out.println("2. Search Skill");
        System.out.println("3. Update Skill");
        System.out.println("4. Delete Skill");
        System.out.println("5. Earn Credits (teach)");
        System.out.println("6. Spend Credits (learn)");
        System.out.println("7. View Profile");
        System.out.println("8. Exit");
    }

    private void addSkill() {
        try {
            System.out.print("Skill name: ");
            String name = sc.nextLine();
            System.out.print("Category (leave blank for General): ");
            String category = sc.nextLine();

            Skill skill = category.isBlank()
                    ? skillService.addSkill(name, "General", currentUser.getName())
                    : skillService.addSkill(name, category, currentUser.getName());

            System.out.println("Skill Registered: " + skill);
            skill.conductSession(); // demonstrates the Teachable interface
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void searchSkill() {
        System.out.print("Enter keyword to search: ");
        String keyword = sc.nextLine();
        List<Skill> results = skillService.searchByName(keyword);
        if (results.isEmpty()) {
            System.out.println("No matching skills found.");
        } else {
            results.forEach(System.out::println);
        }
    }

    private void updateSkill() {
        int id = readInt("Enter skill ID to update: ");
        System.out.print("New skill name: ");
        String name = sc.nextLine();
        System.out.print("New category: ");
        String category = sc.nextLine();
        boolean updated = skillService.updateSkill(id, name, category);
        System.out.println(updated ? "Skill updated." : "Skill ID not found.");
    }

    private void deleteSkill() {
        int id = readInt("Enter skill ID to delete: ");
        boolean deleted = skillService.deleteSkill(id);
        System.out.println(deleted ? "Skill deleted." : "Skill ID not found.");
    }

    private void earnCredits() {
        int amount = readInt("Credits earned for teaching: ");
        creditService.earnCredits(currentUser, amount);
    }

    private void spendCredits() {
        int amount = readInt("Credits to spend: ");
        try {
            creditService.spendCredits(currentUser, amount);
        } catch (InsufficientCreditsException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewProfile() {
        System.out.println(currentUser);
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        while (!sc.hasNextInt()) {
            System.out.print("Please enter a number: ");
            sc.next();
        }
        int value = sc.nextInt();
        sc.nextLine(); // consume leftover newline
        return value;
    }
}
