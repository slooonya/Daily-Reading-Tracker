package com.cpt202.dailyreadingtracker.admin;

import com.cpt202.dailyreadingtracker.security.CustomUserDetailsService.AccountFrozenException;
import com.cpt202.dailyreadingtracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class AdministrationControllerTest {

    @Mock
    private AdministrationService administrationService;

    @InjectMocks
    private AdministrationController administrationController;

    private MockMvc mockMvc;
    private Principal mockPrincipal;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(administrationController).build();
        mockPrincipal = () -> "adminUser";
    }

    // ADC_001
    @Test
    public void testGetAccountFrozenPage() throws Exception {
        mockMvc.perform(get("/account-frozen"))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/account-frozen"));
    }

    // ADC_002
    @Test
    public void testShowSortedUsersWithDefaultSort() throws Exception {
        List<User> mockUsers = Arrays.asList(new User(), new User());
        when(administrationService.getUsersSortedBy("id", "asc")).thenReturn(mockUsers);

        mockMvc.perform(get("/sorted_admin_userlist")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(view().name("administration/admin-user-list"))
                .andExpect(model().attribute("users", mockUsers))
                .andExpect(model().attribute("sortField", "id"))
                .andExpect(model().attribute("sortDirection", "asc"));
    }

    // ADC_003
    @Test
    public void testShowSortedUsersWithCustomSort() throws Exception {
        List<User> mockUsers = Arrays.asList(new User(), new User());
        when(administrationService.getUsersSortedBy("username", "desc")).thenReturn(mockUsers);

        mockMvc.perform(get("/sorted_admin_userlist")
                        .param("sortField", "username")
                        .param("sortDirection", "desc")
                        .principal(mockPrincipal))
                .andExpect(model().attribute("sortField", "username"))
                .andExpect(model().attribute("sortDirection", "desc"));
    }

    // ADC_004
    @Test
    public void testfFreezeUsersWithSelectedIds() throws Exception {
        mockMvc.perform(post("/sorted_admin_userlist/users_freeze")
                        .param("selectedUsers", "1", "2", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sorted_admin_userlist"));

        verify(administrationService).freezeUserByIds(List.of(1L, 2L, 3L));
    }

    // ADC_005
    @Test
    public void testPromoteToAdminWithSelectedIds() throws Exception {
        mockMvc.perform(post("/sorted_admin_userlist/promote_to_admin")
                        .param("selectedUsers", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sorted_admin_userlist"));

        verify(administrationService).promoteUsersToAdmin(List.of(1L, 2L));
    }

    // ADC_006
    @Test
    public void testHandleFrozenAccount() throws Exception {
        when(administrationService.getUsersSortedBy(any(), any()))
                .thenThrow(new AccountFrozenException("Account frozen"));

        mockMvc.perform(get("/sorted_admin_userlist")
                        .principal(mockPrincipal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account-frozen"));
    }

    // ADC_007
    @Test
    public void testPromoteToAdminWithNoSelectedIds() throws Exception {
        mockMvc.perform(post("/sorted_admin_userlist/promote_to_admin"))
                .andExpect(status().is3xxRedirection());

        verify(administrationService, never()).promoteUsersToAdmin(any());
    }

    // ADC_008
    @Test
    public void testShowSortedUsersWithSearchQuery() throws Exception {
        List<User> mockUsers = Collections.singletonList(new User());
        when(administrationService.getUsersSortedBy(any(), any())).thenReturn(mockUsers);

        mockMvc.perform(get("/sorted_admin_userlist")
                        .param("searchQuery", "test")
                        .principal(mockPrincipal))
                .andExpect(model().attributeExists("users"));
    }
}
