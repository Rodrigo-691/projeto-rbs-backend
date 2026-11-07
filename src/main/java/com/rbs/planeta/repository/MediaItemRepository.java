package com.rbs.planeta.repository;

import com.rbs.planeta.model.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaItemRepository extends JpaRepository<MediaItem, UUID> {

    List<MediaItem> findBySponsorIdOrderByDateDesc(UUID sponsorId);

    long countBySponsorId(UUID sponsorId);

    @Query("SELECT COALESCE(SUM(m.reach), 0) FROM MediaItem m WHERE m.sponsor.id = :sponsorId")
    Long calculateTotalReachBySponsorId(UUID sponsorId);
}
