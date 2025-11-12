package com.jakartaone2025.ski.user.service;

import com.jakartaone2025.ski.user.entity.User;
import com.jakartaone2025.ski.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import at.favre.lib.crypto.bcrypt.BCrypt;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * User Service for business logic
 */
@ApplicationScoped
@Transactional
public class UserService {
    
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    
    private final UserRepository userRepository;
    private final Validator validator;
    
    @Inject
    public UserService(UserRepository userRepository, Validator validator) {
        this.userRepository = userRepository;
        this.validator = validator;
    }
    
    /**
     * Create a new user
     */
    public User createUser(User user) {
        validateUser(user);
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UserServiceException("Username already exists: " + user.getUsername());
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserServiceException("Email already exists: " + user.getEmail());
        }
        
        // Hash password
        if (user.getPasswordHash() != null) {
            String hashedPassword = BCrypt.withDefaults().hashToString(12, user.getPasswordHash().toCharArray());
            user.setPasswordHash(hashedPassword);
        }
        
        User savedUser = userRepository.save(user);
        logger.info("Created user: " + savedUser.getUsername());
        return savedUser;
    }
    
    /**
     * Update existing user
     */
    public User updateUser(Long userId, User updatedUser) {
        User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserServiceException("User not found with ID: " + userId));
        
        // Update allowed fields
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setDateOfBirth(updatedUser.getDateOfBirth());
        existingUser.setEmergencyContactName(updatedUser.getEmergencyContactName());
        existingUser.setEmergencyContactPhone(updatedUser.getEmergencyContactPhone());
        existingUser.setSkillLevel(updatedUser.getSkillLevel());
        
        // Email update requires validation
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new UserServiceException("Email already exists: " + updatedUser.getEmail());
            }
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setEmailVerified(false); // Reset email verification
        }
        
        validateUser(existingUser);
        
        User savedUser = userRepository.save(existingUser);
        logger.info("Updated user: " + savedUser.getUsername());
        return savedUser;
    }
    
    /**
     * Update user password
     */
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserServiceException("User not found with ID: " + userId));
        
        // Verify current password
        if (!BCrypt.verifyer().verify(currentPassword.toCharArray(), user.getPasswordHash()).verified) {
            throw new UserServiceException("Invalid current password");
        }
        
        // Hash and save new password
        String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        user.setPasswordHash(hashedPassword);
        
        userRepository.save(user);
        logger.info("Password updated for user: " + user.getUsername());
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Find all users with pagination
     */
    public List<User> findAllUsers(int page, int size) {
        return userRepository.findAll(page, size);
    }
    
    /**
     * Find users by role
     */
    public List<User> findUsersByRole(User.UserRole role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * Search users by name
     */
    public List<User> searchUsers(String searchTerm, int page, int size) {
        return userRepository.searchByName(searchTerm, page, size);
    }
    
    /**
     * Delete user (hard delete)
     */
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserServiceException("User not found with ID: " + userId));
        
        userRepository.delete(user);
        logger.info("Deleted user: " + user.getUsername());
    }
    
    /**
     * Deactivate user (soft delete)
     */
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserServiceException("User not found with ID: " + userId));
        
        user.setActive(false);
        userRepository.save(user);
        logger.info("Deactivated user: " + user.getUsername());
    }
    
    /**
     * Activate user
     */
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserServiceException("User not found with ID: " + userId));
        
        user.setActive(true);
        userRepository.save(user);
        logger.info("Activated user: " + user.getUsername());
    }
    
    /**
     * Verify email
     */
    public void verifyEmail(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserServiceException("User not found with ID: " + userId));
        
        user.setEmailVerified(true);
        userRepository.save(user);
        logger.info("Email verified for user: " + user.getUsername());
    }
    
    /**
     * Update last login timestamp
     */
    public void updateLastLogin(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserServiceException("User not found with ID: " + userId));
        
        user.updateLastLogin();
        userRepository.save(user);
    }
    
    /**
     * Get user statistics
     */
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.countUsers();
        long activeUsers = userRepository.countByRole(User.UserRole.CUSTOMER);
        long instructors = userRepository.countByRole(User.UserRole.INSTRUCTOR);
        long staff = userRepository.countByRole(User.UserRole.STAFF);
        long admins = userRepository.countByRole(User.UserRole.ADMIN);
        
        return new UserStatistics(totalUsers, activeUsers, instructors, staff, admins);
    }
    
    /**
     * Validate user entity
     */
    private void validateUser(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("User validation failed: ");
            for (ConstraintViolation<User> violation : violations) {
                sb.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
            }
            throw new UserServiceException(sb.toString());
        }
    }
    
    /**
     * User Statistics DTO
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long instructors;
        private final long staff;
        private final long admins;
        
        public UserStatistics(long totalUsers, long activeUsers, long instructors, long staff, long admins) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.instructors = instructors;
            this.staff = staff;
            this.admins = admins;
        }
        
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getInstructors() { return instructors; }
        public long getStaff() { return staff; }
        public long getAdmins() { return admins; }
    }
    
    /**
     * User Service Exception
     */
    public static class UserServiceException extends RuntimeException {
        public UserServiceException(String message) {
            super(message);
        }
        
        public UserServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
