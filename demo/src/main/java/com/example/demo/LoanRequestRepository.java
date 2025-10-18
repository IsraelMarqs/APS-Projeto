package com.example.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
    List<LoanRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    @Query("select lr from LoanRequest lr where lr.book.owner = :owner order by lr.createdAt desc")
    List<LoanRequest> findByBookOwnerOrderByCreatedAtDesc(@Param("owner") User owner);
}
