package com.cpt202.dailyreadingtracker.admin;

import com.cpt202.dailyreadingtracker.role.Role;
import com.cpt202.dailyreadingtracker.role.RoleRepository;
import com.cpt202.dailyreadingtracker.user.User;
import com.cpt202.dailyreadingtracker.user.UserRepository;
import com.cpt202.dailyreadingtracker.utils.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdministrationService administrationService;

    private User testUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        adminRole = new Role("ROLE_ADMIN");
        userRole = new Role("ROLE_USER");
    }

    // ADS_001
    @Test
    public void testGetUsersSortedBy() {
        List<User> mockUsers = Arrays.asList(new User(), new User());
        when(userRepository.findAll(any(Sort.class))).thenReturn(mockUsers);

        List<User> result = administrationService.getUsersSortedBy("username", "asc");

        assertEquals(2, result.size());
        verify(userRepository).findAll(Sort.by(Sort.Direction.ASC, "username"));
    }

    // ADS_002
    @Test
    public void testFreezeUserByIdsWithValidIds() {
        List<User> users = Arrays.asList(testUser, new User());
        when(userRepository.findAllById(anyList())).thenReturn(users);

        administrationService.freezeUserByIds(List.of(1L, 2L));

        assertTrue(testUser.isFreezed());
        verify(userRepository, times(2)).save(any());
        verify(emailService).sendAccountFrozenEmail(testUser);
    }

    // ADS_004
    @Test
    public void testPromoteUsersToAdmin() {
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any())).thenReturn(adminRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        administrationService.promoteUsersToAdmin(List.of(1L));

        verify(roleRepository).save(argThat(r -> r.getName().equals("ROLE_ADMIN")));
        assertTrue(testUser.getRoles().contains(adminRole));
    }

    // ADS_005
    @Test
    public void testDemoteUsersFromAdminWithValidUser() {
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        testUser.setRoles(new HashSet<>());
        testUser.getRoles().add(adminRole);

        administrationService.demoteUsersFromAdmin(List.of(1L));

        assertTrue(testUser.getRoles().contains(userRole));
        assertFalse(testUser.getRoles().contains(adminRole));
    }

    // ADS_006
    @Test
    public void testDemoteUsersFromAdminWithMissingUserRole() {
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                administrationService.demoteUsersFromAdmin(List.of(1L)));
    }

    // ADS_007
    @Test
    public void testPromoteUsersToAdminWithUserNotFound() {
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        administrationService.promoteUsersToAdmin(List.of(1L, 2L));

        verify(userRepository).save(testUser);
    }

}

