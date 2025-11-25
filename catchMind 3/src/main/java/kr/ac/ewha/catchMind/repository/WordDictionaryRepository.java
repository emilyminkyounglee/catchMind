package kr.ac.ewha.catchMind.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.ac.ewha.catchMind.model.WordDictionary;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordDictionaryRepository extends JpaRepository<WordDictionary, Long> {

    @Query(value = "SELECT * FROM words WHERE use = true ORDER BY RAND() LIMIT 1", nativeQuery = true)
    WordDictionary getRandom();

    boolean existsByWord(String word);

    @Query("SELECT DISTINCT w.category FROM WordDictionary w WHERE w.category IS NOT NULL")
    List<WordDictionary> findDistinctCategory();
}
