package com.jakartaone2025.ski.user.resource;

import com.jakartaone2025.ski.user.entity.User;
import com.jakartaone2025.ski.user.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * User REST Resource
 */
@Path("/users")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Management", description = "User registration, profile management, and user operations")
public class UserResource {
    
    private static final Logger logger = Logger.getLogger(UserResource.class.getName());
    
    private final UserService userService;
    
    @Context
    private UriInfo uriInfo;
    
    @Inject
    public UserResource(UserService userService) {
        this.userService = userService;
    }
    
    @POST
    @Operation(summary = "Create a new user", description = "Register a new user in the system")
    @APIResponse(responseCode = "201", description = "User created successfully")
    @APIResponse(responseCode = "400", description = "Invalid user data")
    @APIResponse(responseCode = "409", description = "Username or email already exists")
    public Response createUser(@Valid @NotNull User user) {
        try {
            User createdUser = userService.createUser(user);
            URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(createdUser.getId())).build();
            
            // Remove sensitive data from response
            createdUser.setPasswordHash(null);
            
            return Response.created(location)
                .entity(createdUser)
                .build();
                
        } catch (UserService.UserServiceException e) {
            logger.warning("Failed to create user: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("USER_CREATION_FAILED", e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    @APIResponse(responseCode = "200", description = "User found")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response getUserById(@PathParam("id") @Parameter(description = "User ID") Long id) {
        Optional<User> user = userService.findById(id);
        
        if (user.isPresent()) {
            User foundUser = user.get();
            foundUser.setPasswordHash(null); // Remove sensitive data
            return Response.ok(foundUser).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("USER_NOT_FOUND", "User not found with ID: " + id))
                .build();
        }
    }
    
    @GET
    @Operation(summary = "Get all users", description = "Retrieve all users with pagination")
    @APIResponse(responseCode = "200", description = "Users retrieved successfully")
    public Response getAllUsers(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) int size) {
        
        List<User> users = userService.findAllUsers(page, size);
        
        // Remove sensitive data
        users.forEach(user -> user.setPasswordHash(null));
        
        return Response.ok(users).build();
    }
    
    @GET
    @Path("/search")
    @Operation(summary = "Search users", description = "Search users by name, username, or email")
    @APIResponse(responseCode = "200", description = "Search results")
    public Response searchUsers(
            @QueryParam("q") @Parameter(description = "Search term") String searchTerm,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) int size) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_SEARCH", "Search term is required"))
                .build();
        }
        
        List<User> users = userService.searchUsers(searchTerm.trim(), page, size);
        
        // Remove sensitive data
        users.forEach(user -> user.setPasswordHash(null));
        
        return Response.ok(users).build();
    }
    
    @GET
    @Path("/by-role/{role}")
    @Operation(summary = "Get users by role", description = "Retrieve users by their role")
    @APIResponse(responseCode = "200", description = "Users retrieved successfully")
    @APIResponse(responseCode = "400", description = "Invalid role")
    public Response getUsersByRole(@PathParam("role") String roleStr) {
        try {
            User.UserRole role = User.UserRole.valueOf(roleStr.toUpperCase());
            List<User> users = userService.findUsersByRole(role);
            
            // Remove sensitive data
            users.forEach(user -> user.setPasswordHash(null));
            
            return Response.ok(users).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("INVALID_ROLE", "Invalid role: " + roleStr))
                .build();
        }
    }
    
    @PUT
    @Path("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user's profile")
    @APIResponse(responseCode = "200", description = "User updated successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    @APIResponse(responseCode = "400", description = "Invalid user data")
    public Response updateUser(@PathParam("id") Long id, @Valid @NotNull User updatedUser) {
        try {
            User user = userService.updateUser(id, updatedUser);
            user.setPasswordHash(null); // Remove sensitive data
            
            return Response.ok(user).build();
            
        } catch (UserService.UserServiceException e) {
            logger.warning("Failed to update user " + id + ": " + e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("USER_NOT_FOUND", e.getMessage()))
                    .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("USER_UPDATE_FAILED", e.getMessage()))
                    .build();
            }
        }
    }
    
    @PUT
    @Path("/{id}/password")
    @Operation(summary = "Update user password", description = "Update a user's password")
    @APIResponse(responseCode = "204", description = "Password updated successfully")
    @APIResponse(responseCode = "400", description = "Invalid password data")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response updatePassword(@PathParam("id") Long id, @Valid @NotNull PasswordUpdateRequest request) {
        try {
            userService.updatePassword(id, request.getCurrentPassword(), request.getNewPassword());
            return Response.noContent().build();
            
        } catch (UserService.UserServiceException e) {
            logger.warning("Failed to update password for user " + id + ": " + e.getMessage());
            
            if (e.getMessage().contains("not found")) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("USER_NOT_FOUND", e.getMessage()))
                    .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("PASSWORD_UPDATE_FAILED", e.getMessage()))
                    .build();
            }
        }
    }
    
    @PUT
    @Path("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate a user account")
    @APIResponse(responseCode = "204", description = "User deactivated successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response deactivateUser(@PathParam("id") Long id) {
        try {
            userService.deactivateUser(id);
            return Response.noContent().build();
            
        } catch (UserService.UserServiceException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("USER_NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @PUT
    @Path("/{id}/activate")
    @Operation(summary = "Activate user", description = "Activate a user account")
    @APIResponse(responseCode = "204", description = "User activated successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response activateUser(@PathParam("id") Long id) {
        try {
            userService.activateUser(id);
            return Response.noContent().build();
            
        } catch (UserService.UserServiceException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("USER_NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @PUT
    @Path("/{id}/verify-email")
    @Operation(summary = "Verify email", description = "Mark user's email as verified")
    @APIResponse(responseCode = "204", description = "Email verified successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response verifyEmail(@PathParam("id") Long id) {
        try {
            userService.verifyEmail(id);
            return Response.noContent().build();
            
        } catch (UserService.UserServiceException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("USER_NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user account permanently")
    @APIResponse(responseCode = "204", description = "User deleted successfully")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response deleteUser(@PathParam("id") Long id) {
        try {
            userService.deleteUser(id);
            return Response.noContent().build();
            
        } catch (UserService.UserServiceException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("USER_NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/statistics")
    @Operation(summary = "Get user statistics", description = "Retrieve user statistics and counts")
    @APIResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public Response getUserStatistics() {
        UserService.UserStatistics stats = userService.getUserStatistics();
        return Response.ok(stats).build();
    }
    
    /**
     * Password Update Request DTO
     */
    public static class PasswordUpdateRequest {
        @NotNull(message = "Current password is required")
        private String currentPassword;
        
        @NotNull(message = "New password is required")
        private String newPassword;
        
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    /**
     * Error Response DTO
     */
    public static class ErrorResponse {
        private final String code;
        private final String message;
        
        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
