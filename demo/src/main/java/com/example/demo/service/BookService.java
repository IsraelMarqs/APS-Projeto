package com.example.demo.service;

import com.example.demo.entity.Book;
import com.example.demo.entity.User;

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

