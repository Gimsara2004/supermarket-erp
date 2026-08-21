package com.bci.productcrud.config;

import com.bci.productcrud.model.Location;
import com.bci.productcrud.model.Role;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.LocationRepository;
import com.bci.productcrud.repository.RoleRepository;
import com.bci.productcrud.repository.UserRepository;
import com.bci.productcrud.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the small amount of reference data the rest of the app now depends on
 * (a Role for every User, and at least one Location for Inventory/GRN/Sales
 * to be recorded against). Runs once - each insert is guarded so it is safe
 * to start the app repeatedly against the same database.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public DataSeeder(RoleRepository roleRepository, LocationRepository locationRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
        roleRepository.findByName("MANAGER").orElseGet(() -> roleRepository.save(new Role("MANAGER")));
        roleRepository.findByName("CASHIER").orElseGet(() -> roleRepository.save(new Role("CASHIER")));

        if (!locationRepository.existsByLocationName("Main Warehouse")) {
            Location main = new Location();
            main.setLocationName("Main Warehouse");
            main.setAddress("Head office / default receiving location");
            locationRepository.save(main);
        }

        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setFullName("System Administrator");
            admin.setUsername("admin");
            admin.setPassword(PasswordUtil.hash("admin123"));
            admin.setRole(adminRole);
            admin.setStatus("ACTIVE");
            userRepository.save(admin);
        }
    }
}
