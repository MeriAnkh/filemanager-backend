package com.filemanager.repository;

import com.filemanager.entity.Report;
import com.filemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByOwnerOrderByCreatedAtDesc(User owner);
    Optional<Report> findByIdAndOwner(UUID id, User owner);
}
