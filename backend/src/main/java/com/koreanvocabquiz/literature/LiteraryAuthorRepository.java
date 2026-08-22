package com.koreanvocabquiz.literature;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LiteraryAuthorRepository extends JpaRepository<LiteraryAuthor, Long> {
    List<LiteraryAuthor> findAllByOrderByNameAsc();
    Optional<LiteraryAuthor> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
