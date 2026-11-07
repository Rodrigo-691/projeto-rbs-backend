package com.rbs.planeta.repository;

import com.rbs.planeta.model.SponsorFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SponsorFileRepository extends JpaRepository<SponsorFile, UUID> {

    List<SponsorFile> findBySponsorIdOrderByCreatedAtDesc(UUID sponsorId);

    List<SponsorFile> findBySponsorIdAndTabKeyOrderByCreatedAtDesc(UUID sponsorId, String tabKey);

    List<SponsorFile> findBySponsorIdAndTabKeyAndCategoryOrderByCreatedAtDesc(UUID sponsorId, String tabKey, String category);

    long countBySponsorId(UUID sponsorId);
}
