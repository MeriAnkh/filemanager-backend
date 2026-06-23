package com.filemanager.repository;

import com.filemanager.entity.WuTransaction;
import com.filemanager.entity.WuTransaction.TransactionNature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WuTransactionRepository extends JpaRepository<WuTransaction, UUID> {
    List<WuTransaction> findByWuReportIdAndNatureOrderByDateAscTimeAsc(
            UUID wuReportId, TransactionNature nature);
}
