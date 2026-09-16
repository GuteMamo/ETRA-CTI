package com.etra.cti.organization;

import com.etra.cti.common.exception.NotFoundException;
import com.etra.cti.organization.dto.AddMemberRequest;
import com.etra.cti.organization.dto.CreateOrganizationRequest;
import com.etra.cti.organization.dto.OrganizationDto;
import com.etra.cti.user.User;
import com.etra.cti.user.UserRepository;
import com.etra.cti.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository orgs;
    private final OrganizationMemberRepository members;
    private final UserRepository users;

    @Transactional
    public OrganizationDto create(CreateOrganizationRequest req, UUID creatorId) {
        if (orgs.existsBySlug(req.slug())) {
            throw new IllegalArgumentException("Slug already taken");
        }
        Organization o = new Organization();
        o.setName(req.name());
        o.setSlug(req.slug().toLowerCase());
        orgs.save(o);

        OrganizationMember m = new OrganizationMember();
        m.setOrganizationId(o.getId());
        m.setUserId(creatorId);
        m.setRole(UserRole.ADMIN);
        members.save(m);

        return OrganizationDto.from(o, UserRole.ADMIN);
    }

    public List<OrganizationDto> listMine(UUID userId) {
        return members.findByUserId(userId).stream()
            .map(m -> {
                Organization o = orgs.findById(m.getOrganizationId()).orElseThrow();
                return OrganizationDto.from(o, m.getRole());
            })
            .toList();
    }

    public OrganizationDto get(UUID orgId, UUID userId) {
        OrganizationMember m = requireMember(orgId, userId);
        Organization o = orgs.findById(orgId).orElseThrow(() -> new NotFoundException("Organization not found"));
        return OrganizationDto.from(o, m.getRole());
    }

    @Transactional
    public void addMember(UUID orgId, AddMemberRequest req, UUID actorId) {
        requireAdmin(orgId, actorId);
        if (members.findByOrganizationIdAndUserId(orgId, req.userId()).isPresent()) {
            throw new IllegalArgumentException("User already member");
        }
        User u = users.findById(req.userId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        OrganizationMember m = new OrganizationMember();
        m.setOrganizationId(orgId);
        m.setUserId(u.getId());
        m.setRole(req.role());
        members.save(m);
    }

    public OrganizationMember requireMember(UUID orgId, UUID userId) {
        return members.findByOrganizationIdAndUserId(orgId, userId)
            .orElseThrow(() -> new AccessDeniedException("Not a member of this organization"));
    }

    public OrganizationMember requireAdmin(UUID orgId, UUID userId) {
        OrganizationMember m = requireMember(orgId, userId);
        if (m.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Admin role required");
        }
        return m;
    }
}
