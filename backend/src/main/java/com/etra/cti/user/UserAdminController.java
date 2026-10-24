package com.etra.cti.user;

import com.etra.cti.auth.JwtAuthFilter;
import com.etra.cti.auth.dto.RegisterRequest;
import com.etra.cti.auth.dto.UserDto;
import com.etra.cti.auth.AuthService;
import com.etra.cti.audit.AuditService;
import com.etra.cti.common.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserRepository users;
    private final AuthService auth;
    private final AuditService audit;
    private final com.etra.cti.organization.OrganizationRepository orgs;
    private final com.etra.cti.organization.OrganizationMemberRepository orgMembers;

    public UserAdminController(UserRepository users,
                               AuthService auth,
                               AuditService audit,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) com.etra.cti.organization.OrganizationRepository orgs,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) com.etra.cti.organization.OrganizationMemberRepository orgMembers) {
        this.users = users;
        this.auth = auth;
        this.audit = audit;
        this.orgs = orgs;
        this.orgMembers = orgMembers;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public List<UserDto> list() {
        return users.findAll().stream().map(UserDto::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECURITY_MANAGER')")
    public UserDto get(@PathVariable UUID id) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return UserDto.from(u);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto create(@Valid @RequestBody CreateUserRequest req,
                          @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        String cleanEmail = req.email().trim().toLowerCase();
        String cleanName = req.fullName().trim();
        UserRole targetRole = UserRole.VIEWER;
        if (req.role() != null && !req.role().isBlank()) {
            try {
                targetRole = UserRole.valueOf(req.role().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                targetRole = UserRole.VIEWER;
            }
        }
        UserDto created = auth.register(new RegisterRequest(cleanEmail, req.password(), cleanName));
        User u = users.findById(created.id()).orElseThrow();
        u.setRole(targetRole);
        users.save(u);

        if (orgMembers != null) {
            List<com.etra.cti.organization.OrganizationMember> myMemberships = (me != null && me.id() != null)
                ? orgMembers.findByUserId(me.id()) : List.of();
            if (!myMemberships.isEmpty()) {
                for (var om : myMemberships) {
                    if (orgMembers.findByOrganizationIdAndUserId(om.getOrganizationId(), u.getId()).isEmpty()) {
                        var newMember = new com.etra.cti.organization.OrganizationMember();
                        newMember.setOrganizationId(om.getOrganizationId());
                        newMember.setUserId(u.getId());
                        newMember.setRole(targetRole);
                        orgMembers.save(newMember);
                    }
                }
            } else if (orgs != null) {
                for (var o : orgs.findAll()) {
                    if (orgMembers.findByOrganizationIdAndUserId(o.getId(), u.getId()).isEmpty()) {
                        var newMember = new com.etra.cti.organization.OrganizationMember();
                        newMember.setOrganizationId(o.getId());
                        newMember.setUserId(u.getId());
                        newMember.setRole(targetRole);
                        orgMembers.save(newMember);
                    }
                }
            }
        }

        audit.record(me != null ? me.id() : null, "USER_CREATED_BY_ADMIN", "User", u.getId().toString(),
            Map.of("email", u.getEmail(), "role", u.getRole().name()));
        return UserDto.from(u);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateRole(@PathVariable UUID id,
                              @RequestBody Map<String, String> body,
                              @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        UserRole oldRole = u.getRole();
        String roleStr = body.get("role");
        UserRole newRole = oldRole;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                newRole = UserRole.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        u.setRole(newRole);
        users.save(u);
        audit.recordChange(me != null ? me.id() : null, "USER_ROLE_CHANGED", "User", id.toString(),
            Map.of("role", oldRole.name()),
            Map.of("role", newRole.name()));
        return UserDto.from(u);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateStatus(@PathVariable UUID id,
                                @RequestBody Map<String, Boolean> body,
                                @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        if (me != null && id.equals(me.id()) && Boolean.FALSE.equals(body.get("enabled"))) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        boolean oldStatus = u.isEnabled();
        boolean newStatus = body.getOrDefault("enabled", true);
        u.setEnabled(newStatus);
        users.save(u);
        audit.recordChange(me != null ? me.id() : null, "USER_STATUS_CHANGED", "User", id.toString(),
            Map.of("enabled", oldStatus),
            Map.of("enabled", newStatus));
        return UserDto.from(u);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id,
                       @AuthenticationPrincipal JwtAuthFilter.AuthedUser me) {
        // Prevent self-deletion
        if (me != null && id.equals(me.id())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));

        audit.record(me != null ? me.id() : null, "USER_DELETED", "User", id.toString(),
            Map.of("email", u.getEmail(), "fullName", u.getFullName(), "role", u.getRole().name()));

        users.delete(u);
    }
}
