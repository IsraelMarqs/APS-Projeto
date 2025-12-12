package com.example.demo.repository;

import java.util.List;

import com.example.demo.entity.Book;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findAllByOrderByTitleAsc();
    List<Book> findByOwner(User owner);
    List<Book> findByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCaseOrderByTitleAsc(String title, String authors);
    List<Book> findByAvailableTrueOrderByTitleAsc();
    // Busca TODOS os livros do dono (Disponíveis ou não) para ele gerenciar
    List<Book> findByOwnerOrderByTitleAsc(User owner);
}
