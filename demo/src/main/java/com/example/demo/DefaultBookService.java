package com.example.demo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DefaultBookService implements BookService {

    private final BookRepository bookRepository;

    public DefaultBookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<Book> findAllOrdered() {
        return bookRepository.findAllByOrderByTitleAsc();
    }

    @Override
    public List<Book> search(String q) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorsContainingIgnoreCaseOrderByTitleAsc(q, q);
    }   

    @Override
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public void delete(Book book) {
        bookRepository.delete(book);
    }

    @Override
    public List<Book> findByOwner(User owner) {
        return bookRepository.findByOwner(owner);
    }
}

