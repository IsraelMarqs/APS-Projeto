package com.example.demo.repository;

import java.util.Collection;
import java.util.List;

import com.example.demo.entity.Book;
import com.example.demo.entity.LoanRequest;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    List<LoanRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    @Query("select lr from LoanRequest lr where lr.book.owner = :owner order by lr.createdAt desc")
    List<LoanRequest> findByBookOwnerOrderByCreatedAtDesc(@Param("owner") User owner);

    List<LoanRequest> findByBookAndStatus(Book book, LoanRequest.Status status);

    // --- CORREÇÃO: Adicionamos @Query para garantir que o Spring entenda a relação ---

    // 1. Conta pedidos PENDENTES para os meus livros
    @Query("select count(lr) from LoanRequest lr where lr.book.owner = :owner and lr.status = :status")
    long countByBookOwnerAndStatus(@Param("owner") User owner, @Param("status") LoanRequest.Status status);

    // 2. Conta respostas NÃO VISTAS para meus pedidos
    @Query("select count(lr) from LoanRequest lr where lr.requester = :requester and lr.status in :statuses and lr.seen = false")
    long countByRequesterAndStatusInAndSeenFalse(@Param("requester") User requester, @Param("statuses") Collection<LoanRequest.Status> statuses);

    // 3. Busca respostas NÃO VISTAS (para limpar depois)
    @Query("select lr from LoanRequest lr where lr.requester = :requester and lr.status in :statuses and lr.seen = false")
    List<LoanRequest> findByRequesterAndStatusInAndSeenFalse(@Param("requester") User requester, @Param("statuses") Collection<LoanRequest.Status> statuses);
}