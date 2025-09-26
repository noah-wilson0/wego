package com.wego.wego.external.tourapi.place.repository;

import com.wego.wego.domain.place.repository.PlaceQueryRepository;
import com.wego.wego.external.tourapi.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, PlaceQueryRepository {

    Place findByTitle(String title);
    List<Place> findAllByContentIdIn(List<String> contentIds);
    Optional<Place> findByContentId(String contentId);
    List<Place> findByContentIdIn(List<String> contentIds);

    @Query(value = """
    SELECT *
    FROM place
    WHERE city_code_id = :cityCodeId
      AND similarity(title, :title) > 0.3
    ORDER BY similarity(title, :title) DESC
    LIMIT 1
""", nativeQuery = true)
    Optional<Place> findMostSimilarTitleInCity(@Param("title") String title, @Param("cityCodeId") Long cityCodeId);


    @Query(value = """
    SELECT *
    FROM place
    WHERE similarity(title, :title) > 0.3
    ORDER BY similarity(title, :title) DESC
    LIMIT 1
""", nativeQuery = true)
    Optional<Place> findMostSimilarTitle(@Param("title") String title);

    //ai db 비교 테스트용
    List<Place> findByTitleContaining(String title);

    @Query("SELECT p FROM Place p WHERE TRIM(CONCAT(p.addr1, ' ', COALESCE(p.addr2, ''))) = :fullAddr")
    List<Place> findByFullAddress(@Param("fullAddr") String fullAddr);


    Page<Place> findByPlaceType(String placeType, Pageable pageable);
    @Query("select p from Place p where p.placeType = :placeType and p.cityCode.cityCodeId in :cityCodeIds")
    Page<Place> findByPlaceTypeAndCityCodeIdIn(String placeType, List<Integer> cityCodeIds, Pageable pageable);




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

