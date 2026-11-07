package com.rbs.planeta.repository;

import com.rbs.planeta.model.SponsorMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SponsorMemberRepository extends JpaRepository<SponsorMember, Long> {

    List<SponsorMember> findBySponsorIdOrderByEmailAsc(UUID sponsorId);

    Optional<SponsorMember> findBySponsorIdAndEmailIgnoreCase(UUID sponsorId, String email);

    void deleteBySponsorIdAndEmailIgnoreCase(UUID sponsorId, String email);

    boolean existsBySponsorIdAndEmailIgnoreCase(UUID sponsorId, String email);
}
