package com.cpt202.dailyreadingtracker.admin;

import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.cpt202.dailyreadingtracker.role.Role;
import com.cpt202.dailyreadingtracker.role.RoleRepository;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.EmailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdministrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    public List<User> getUsersSortedBy(String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        return userRepository.findAll(sort);
    }

    @Transactional
    public void freezeUserByIds(List<Long> UserIds) {
        List<User> users = userRepository.findAllById(UserIds);
        for(User user: users){
            user.freezeUser();
            emailService.sendAccountFrozenEmail(user);
            userRepository.save(user);
        }
    }

    @Transactional
    public void unfreezeUserByIds(List<Long> UserIds) {
        List<User> users = userRepository.findAllById(UserIds);
        for(User user: users){
            user.unfreezeUser();
            userRepository.save(user);
        }
    }

    @Transactional
    public void promoteUsersToAdmin(List<Long> userIds) {
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseGet(() -> {
                Role newRole = new Role("ROLE_ADMIN");
                return roleRepository.save(newRole);
            });


        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {

                if (user.getRoles() == null)
                    user.setRoles(new HashSet<>());
                else user.getRoles().clear();

                user.getRoles().add(adminRole);
                userRepository.save(user);
            }
        }
    }

    @Transactional
    public void demoteUsersFromAdmin(List<Long> userIds) {
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        if (userRole == null) {
            userRole = new Role();
            userRole.setName("ROLE_USER");
            userRole = roleRepository.save(userRole);
        }

        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                if (user.getRoles() == null)
                    user.setRoles(new HashSet<>());
                else user.getRoles().clear();

                user.getRoles().add(userRole);
                userRepository.save(user);
            }
        }
    }
}
