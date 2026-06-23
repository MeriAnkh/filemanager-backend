package com.filemanager.repository;

import com.filemanager.entity.User;
import com.filemanager.entity.WuReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WuReportRepository extends JpaRepository<WuReport, UUID> {
    List<WuReport> findByOwnerOrderByReportDateDesc(User owner);
    Optional<WuReport> findByIdAndOwner(UUID id, User owner);
}
