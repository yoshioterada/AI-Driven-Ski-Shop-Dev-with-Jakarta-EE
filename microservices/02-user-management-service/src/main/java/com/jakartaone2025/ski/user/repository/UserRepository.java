package com.jakartaone2025.ski.user.repository;

import com.jakartaone2025.ski.user.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.inject.Inject;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * User Repository for database operations
 */
@ApplicationScoped
@Transactional
public class UserRepository {
    
    private static final Logger logger = Logger.getLogger(UserRepository.class.getName());
    
    @Inject
    private EntityManager entityManager;
    
    /**
     * Save or update user
     */
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            logger.info("Created new user: " + user.getUsername());
            return user;
        } else {
            User updated = entityManager.merge(user);
            logger.info("Updated user: " + user.getUsername());
            return updated;
        }
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        User user = entityManager.find(User.class, id);
        return Optional.ofNullable(user);
    }
    
    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        try {
            TypedQuery<User> query = entityManager.createNamedQuery("User.findByUsername", User.class);
            query.setParameter("username", username);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        try {
            TypedQuery<User> query = entityManager.createNamedQuery("User.findByEmail", User.class);
            query.setParameter("email", email);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Find all active users
     */
    public List<User> findActiveUsers() {
        TypedQuery<User> query = entityManager.createNamedQuery("User.findActiveUsers", User.class);
        return query.getResultList();
    }
    
    /**
     * Find users by role
     */
    public List<User> findByRole(User.UserRole role) {
        TypedQuery<User> query = entityManager.createNamedQuery("User.findByRole", User.class);
        query.setParameter("role", role);
        return query.getResultList();
    }
    
    /**
     * Find all users with pagination
     */
    public List<User> findAll(int page, int size) {
        TypedQuery<User> query = entityManager.createQuery(
            "SELECT u FROM User u ORDER BY u.createdAt DESC", User.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }
    
    /**
     * Count total users
     */
    public long countUsers() {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u", Long.class);
        return query.getSingleResult();
    }
    
    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
        query.setParameter("username", username);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
    
    /**
     * Delete user by ID
     */
    public boolean deleteById(Long id) {
        Optional<User> user = findById(id);
        if (user.isPresent()) {
            entityManager.remove(user.get());
            logger.info("Deleted user with ID: " + id);
            return true;
        }
        return false;
    }
    
    /**
     * Soft delete user (set active = false)
     */
    public boolean softDeleteById(Long id) {
        Optional<User> userOpt = findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(false);
            save(user);
            logger.info("Soft deleted user with ID: " + id);
            return true;
        }
        return false;
    }
    
    /**
     * Search users by name
     */
    public List<User> searchByName(String searchTerm, int page, int size) {
        TypedQuery<User> query = entityManager.createQuery(
            "SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(:searchTerm) OR " +
            "LOWER(u.lastName) LIKE LOWER(:searchTerm) OR " +
            "LOWER(u.username) LIKE LOWER(:searchTerm) " +
            "ORDER BY u.lastName, u.firstName", User.class);
        
        query.setParameter("searchTerm", "%" + searchTerm + "%");
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        
        return query.getResultList();
    }
    
    /**
     * Count users by role
     */
    public long countByRole(User.UserRole role) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.role = :role", Long.class);
        query.setParameter("role", role);
        return query.getSingleResult();
    }
}
