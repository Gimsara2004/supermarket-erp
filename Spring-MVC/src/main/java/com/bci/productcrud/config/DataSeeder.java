package com.bci.productcrud.config;

import com.bci.productcrud.model.Role;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.RoleRepository;
import com.bci.productcrud.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * On a brand-new database there are no Roles or Users yet, which would mean
 * nobody could ever log in. This creates one default ADMIN role and one
 * default admin account on first startup only (it does nothing if any user
 * already exists), so there's always a way in.
 *
 * Default login: username "admin", password "admin123".
 * Change this password (or create your own user and delete this one) after first login.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(newRole("ADMIN")));
        roleRepository.findByName("STAFF").orElseGet(() -> roleRepository.save(newRole("STAFF")));

        User admin = new User();
        admin.setFullName("System Administrator");
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setRole(adminRole);
        admin.setEmail("admin@example.com");
        admin.setStatus("ACTIVE");
        userRepository.save(admin);

        System.out.println("=====================================================");
        System.out.println(" No users found - created a default admin account:");
        System.out.println("   username: admin");
        System.out.println("   password: admin123");
        System.out.println(" Log in and create your own users, then feel free to");
        System.out.println(" delete or change this default account.");
        System.out.println("=====================================================");
    }

    private Role newRole(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
