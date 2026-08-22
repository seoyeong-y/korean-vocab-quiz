package com.koreanvocabquiz.literature;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LiteraryFeatureRepository extends JpaRepository<LiteraryFeature, Long> {
    List<LiteraryFeature> findAllByOrderByIdAsc();
    List<LiteraryFeature> findByAuthorId(Long authorId);
    List<LiteraryFeature> findByWorkId(Long workId);
    boolean existsByAuthorIdAndWorkIdAndContent(Long authorId, Long workId, String content);
    boolean existsByAuthorIdAndWorkIdAndContentAndIdNot(Long authorId, Long workId, String content, Long id);
}
