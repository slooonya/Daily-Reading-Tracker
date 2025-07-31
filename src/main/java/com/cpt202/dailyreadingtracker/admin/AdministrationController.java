package com.cpt202.dailyreadingtracker.admin;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cpt202.dailyreadingtracker.security.CustomUserDetailsService.AccountFrozenException;
import com.cpt202.dailyreadingtracker.user.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Controller responsible for managing administrative tasks.
 * <p>
 * Provides endpoints for:
 * <ul>
 *     <li>Viewing administrative pages such as account frozen, admin home, and violation logs</li>
 *     <li>Sorting and displaying user lists</li>
 *     <li>Freezing and unfreezing user accounts</li>
 *     <li>Promoting and demoting users to/from admin roles</li>
 * </ul>
 * <p>
 */

@Controller
@RequiredArgsConstructor
public class AdministrationController {

    private final AdministrationService administrationService;

    @GetMapping("/account-frozen")
    public String getAccountFrozenPage() {
        return "admin/account-frozen"; 
    }

    @GetMapping("/homeforadmin")
    public String getAdminHome() {
        return "admin/homeforadmin";
    }

    @GetMapping("/viologs")
    public String getViolationLogs() {
        return "admin/violationlog";
    }

    @GetMapping("/sorted_admin_userlist")
    public String showSortedUsers(
        @RequestParam(required = false) String searchQuery,
        @RequestParam(name = "sortField", defaultValue = "id") String sortField,
        @RequestParam(name = "sortDirection", defaultValue = "asc") String sortDirection,
        Model m, Principal principal) {
            List<User> usersinlist = administrationService.getUsersSortedBy(sortField, sortDirection);

            m.addAttribute("users", usersinlist);
            m.addAttribute("sortField", sortField);
            m.addAttribute("sortDirection", sortDirection);

            return "admin/admin-user-list";
        }

    @PostMapping("/sorted_admin_userlist/users_freeze")
    public String freezeTheUser(@RequestParam(name = "selectedUsers") List<Long> selectedUserIds, RedirectAttributes redirectAttributes) {
        administrationService.freezeUserByIds(selectedUserIds);
        return "redirect:/sorted_admin_userlist"; 
    }


    @PostMapping("/sorted_admin_userlist/users_unfreeze")
    public String unfreezeTheUser(@RequestParam(name = "selectedUsers") List<Long> selectedUserIds) {
        administrationService.unfreezeUserByIds(selectedUserIds);
        return "redirect:/sorted_admin_userlist"; 
    }

    @PostMapping("/sorted_admin_userlist/promote_to_admin")
    @Transactional
    public String promoteToAdmin(@RequestParam(name = "selectedUsers", required = false) List<Long> selectedUserIds) {
        if (selectedUserIds != null && !selectedUserIds.isEmpty()) {
            administrationService.promoteUsersToAdmin(selectedUserIds);
        }
        return "redirect:/sorted_admin_userlist";
    }

    @PostMapping("/sorted_admin_userlist/demote_from_admin")
    @Transactional
    public String demoteFromAdmin(@RequestParam(required = false) List<Long> selectedUsers) {
        if (selectedUsers != null && !selectedUsers.isEmpty()) {
            administrationService.demoteUsersFromAdmin(selectedUsers);
        }
        return "redirect:/sorted_admin_userlist";
    }

    @ExceptionHandler(AccountFrozenException.class)
    public String handleFrozenAccount(AccountFrozenException ex) {
        return "redirect:/account-frozen";    
    }
}
