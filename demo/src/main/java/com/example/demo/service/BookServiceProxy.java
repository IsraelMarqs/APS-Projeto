package com.example.demo.service;

import com.example.demo.entity.Book;
import com.example.demo.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy for BookService that adds a simple in-memory cache for books by id
 * and logs access. It delegates to DefaultBookService for actual DB operations.
 * Cache is invalidated on save/delete to avoid stale data.
 */
@Service
@Primary
public class BookServiceProxy implements BookService {

    private final Logger logger = LoggerFactory.getLogger(BookServiceProxy.class);
    private final BookService delegate;

    // simple cache by id
    private final Map<Long, Book> cacheById = new ConcurrentHashMap<>();

    public BookServiceProxy(DefaultBookService delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Book> findAllOrdered() {
        logger.debug("BookServiceProxy: findAllOrdered() called");
        // don't cache full lists to keep implementation simple; just delegate
        return delegate.findAllOrdered();
    }

    @Override
    public List<Book> search(String q) {
        logger.debug("BookServiceProxy: search('{}') called", q);
        return delegate.search(q);
    }

    @Override
    public Optional<Book> findById(Long id) {
        if (id == null) return Optional.empty();
        Book cached = cacheById.get(id);
        if (cached != null) {
            logger.debug("BookServiceProxy: cache hit for id={}", id);
            return Optional.of(cached);
        }
        logger.debug("BookServiceProxy: cache miss for id={}", id);
        Optional<Book> b = delegate.findById(id);
        b.ifPresent(book -> cacheById.put(book.getId(), book));
        return b;
    }

    @Override
    public Book save(Book book) {
        Book saved = delegate.save(book);
        if (saved != null && saved.getId() != null) {
            cacheById.put(saved.getId(), saved);
            logger.debug("BookServiceProxy: updated cache for id={}", saved.getId());
        }
        return saved;
    }

    @Override
    public void delete(Book book) {
        if (book != null && book.getId() != null) {
            cacheById.remove(book.getId());
            logger.debug("BookServiceProxy: removed id={} from cache", book.getId());
        }
        delegate.delete(book);
    }

    @Override
    public List<Book> findByOwner(User owner) {
        if (owner == null) return Collections.emptyList();
        // delegate and optionally cache results by id
        List<Book> list = delegate.findByOwner(owner);
        for (Book b : list) {
            if (b.getId() != null) cacheById.put(b.getId(), b);
        }
        return list;
    }
}
