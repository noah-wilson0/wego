package com.wego.wego.external.tourapi.place.repository;

import com.wego.wego.external.tourapi.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Place findByTitle(String title);
    List<Place> findAllByContentIdIn(List<String> contentIds);
    Optional<Place> findByContentId(String contentId);

    @Query(value = """
    SELECT *
    FROM place
    WHERE city_code_id = :cityCodeId
      AND similarity(title, :title) > 0.3
    ORDER BY similarity(title, :title) DESC
    LIMIT 1
""", nativeQuery = true)
    Optional<Place> findMostSimilarTitleInCity(@Param("title") String title, @Param("cityCodeId") Long cityCodeId);

    //ai db 비교 테스트용
    List<Place> findByTitleContaining(String title);

    @Query("SELECT p FROM Place p WHERE TRIM(CONCAT(p.addr1, ' ', COALESCE(p.addr2, ''))) = :fullAddr")
    List<Place> findByFullAddress(@Param("fullAddr") String fullAddr);


    Page<Place> findByPlaceType(String placeType, Pageable pageable);



    //유사도 테스트용
    @Query(value = """
    SELECT title, similarity(title, :title) AS sim
    FROM place
    WHERE similarity(title, :title) > 0.4
    ORDER BY sim DESC
    LIMIT 1
""", nativeQuery = true)
    List<Object[]> findMostSimilarTitleWithScore(@Param("title") String title);


}

