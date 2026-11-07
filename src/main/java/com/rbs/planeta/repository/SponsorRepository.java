package com.rbs.planeta.repository;

import com.rbs.planeta.model.QuotaLevel;
import com.rbs.planeta.model.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, UUID> {

    List<Sponsor> findAllByOrderByNameAsc();

    List<Sponsor> findByQuotaOrderByNameAsc(QuotaLevel quota);

    @Query("SELECT s FROM Sponsor s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.segment) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY s.name ASC")
    List<Sponsor> searchByNameOrSegment(String query);

    @Query("SELECT DISTINCT s FROM Sponsor s JOIN s.members m WHERE LOWER(m.email) = LOWER(:email)")
    List<Sponsor> findByMemberEmail(String email);
}
