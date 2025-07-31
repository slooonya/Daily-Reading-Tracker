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

/**
 * Service responsible for administrative operations on users and roles.
 * <ul>
 *     <li>Retrieve users sorted by a specific field and direction</li>
 *     <li>Freeze user accounts and notify them via email</li>
 *     <li>Unfreeze user accounts</li>
 *     <li>Promote users to admin roles</li>
 *     <li>Demote users from admin roles</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class AdministrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    /**
     * Retrieves a list of users sorted by the specified field and direction.
     *
     * @param sortField     the field to sort by (e.g., "username", "email")
     * @param sortDirection the direction of sorting ("ASC" for ascending, "DESC" for descending)
     * @return a list of users sorted by the specified criteria
     */
    public List<User> getUsersSortedBy(String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        return userRepository.findAll(sort);
    }

    /**
     * Bans user accounts by their IDs and sends a notification email to each user.
     *
     * @param userIds a list of user IDs whose accounts should be banned
     */
    @Transactional
    public void freezeUserByIds(List<Long> UserIds) {
        List<User> users = userRepository.findAllById(UserIds);
        for(User user: users){
            user.freezeUser();
            emailService.sendAccountFrozenEmail(user);
            userRepository.save(user);
        }
    }

    /**
     * Unbans user accounts by their IDs.
     *
     * @param userIds a list of user IDs whose accounts should be unbbanned
     */
    @Transactional
    public void unfreezeUserByIds(List<Long> UserIds) {
        List<User> users = userRepository.findAllById(UserIds);
        for(User user: users){
            user.unfreezeUser();
            userRepository.save(user);
        }
    }

    /**
     * Promotes users to admin roles by assigning them the "ROLE_ADMIN".
     *
     * @param userIds a list of user IDs to promote to admin
     */
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

    /**
     * Demotes admin users to regular users by assigning them the "ROLE_USER".
     *
     * @param userIds a list of user IDs to demote from admin
     */
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
