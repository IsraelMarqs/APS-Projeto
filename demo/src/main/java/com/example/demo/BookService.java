package com.example.demo;

import java.util.List;
import java.util.Optional;

public interface BookService {
    List<Book> findAllOrdered();
    List<Book> search(String q);
    Optional<Book> findById(Long id);
    Book save(Book book);
    void delete(Book book);
    List<Book> findByOwner(User owner);
}

