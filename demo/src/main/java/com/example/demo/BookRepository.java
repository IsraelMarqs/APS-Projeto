package com.example.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findAllByOrderByTitleAsc();
    List<Book> findByOwner(User owner);
    List<Book> findByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCaseOrderByTitleAsc(String title, String authors);
    List<Book> findByAvailableTrueOrderByTitleAsc();
}
