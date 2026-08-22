package com.koreanvocabquiz.literature;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LiteraryWorkRepository extends JpaRepository<LiteraryWork, Long> {
    List<LiteraryWork> findAllByOrderByTitleAsc();
    List<LiteraryWork> findByAuthorIdOrderByTitleAsc(Long authorId);
    Optional<LiteraryWork> findByAuthorIdAndTitle(Long authorId, String title);
    boolean existsByAuthorIdAndTitle(Long authorId, String title);
    boolean existsByAuthorIdAndTitleAndIdNot(Long authorId, String title, Long id);
}
