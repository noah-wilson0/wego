package com.wego.wego.domain.place.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import com.wego.wego.domain.plan.dto.draft.auto.GeminiPlaceItemResponse;
import com.wego.wego.external.tourapi.place.entity.Place;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.wego.wego.external.tourapi.place.entity.QPlace.place;


public class PlaceQueryRepositoryImpl implements PlaceQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public PlaceQueryRepositoryImpl(EntityManager em) {
        this.jpaQueryFactory = new JPAQueryFactory(em);
    }


    @Override
    public Page<DraftPlanPlaceResponse> searchByTitleInCities(List<String> placeTypes, List<Long> cityCodeIds, String keyword, Pageable pageable) {

        List<DraftPlanPlaceResponse> places = jpaQueryFactory
                .select(Projections.constructor(DraftPlanPlaceResponse.class,
                        place.contentId,
                        place.title,
                        place.image,
                        place.placeType,
                        place.addr1,
                        toDoubleOrZero(place.longitude),
                        toDoubleOrZero(place.latitude),
                        place.averageRating,
                        place.likeCount
                ))
                .distinct()
                .from(place)
                .where(placeTypeIn(placeTypes),
                        cityCodeIn(cityCodeIds),
                        titleLike(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(place.averageRating.desc(), place.likeCount.desc())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory
                .select(place.id)
                .from(place)
                .where(placeTypeIn(placeTypes),
                        cityCodeIn(cityCodeIds),
                        titleLike(keyword));


        return PageableExecutionUtils.getPage(places,pageable,count::fetchCount);
    }

    @Override
    public Page<GeminiPlaceItemResponse> searchGeminiPlaceItemResponseByTitleInCities(List<String> placeTypes, List<Long> cityCodeIds, Pageable pageable) {
        List<GeminiPlaceItemResponse> places = jpaQueryFactory
                .select(Projections.constructor(GeminiPlaceItemResponse.class,
                        place.contentId,
                        place.title,
                        place.image,
                        place.addr1,
                        toDoubleOrZero(place.longitude),
                        toDoubleOrZero(place.latitude),
                        place.averageRating,
                        place.likeCount))
                .distinct()
                .from(place)
                .where(placeTypeIn(placeTypes),
                        cityCodeIn(cityCodeIds))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(place.averageRating.desc(), place.likeCount.desc())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory
                .select(place.id)
                .from(place)
                .where(placeTypeIn(placeTypes),
                        cityCodeIn(cityCodeIds));
        return PageableExecutionUtils.getPage(places,pageable,count::fetchCount);

    }



    private static BooleanExpression cityCodeIn(List<Long> cityCodeIds) {
        return CollectionUtils.isEmpty(cityCodeIds) ? null : place.cityCode.cityCodeId.in(cityCodeIds);
    }

    private static BooleanExpression titleLike(String keyword) {
        return StringUtils.hasText(keyword) ? place.title.containsIgnoreCase(keyword.trim()) : null;
    }

    private static BooleanExpression placeTypeIn(List<String> placeTypes) {
        return CollectionUtils.isEmpty(placeTypes) ? null : place.placeType.in(placeTypes);
    }

    private static NumberExpression<Double> toDoubleOrZero(StringPath path) {
        // HQL 표준: cast(nullif(...,'') as double), null이면 0.0
        return Expressions.numberTemplate(
                Double.class,
                "coalesce(cast(nullif({0}, '') as double), 0.0)",
                path
        );
    }

    private static NumberExpression<Double> similarityExpr(StringPath field, String title) {
        // similarity(field, :title)
        return Expressions.numberTemplate(
                Double.class,
                "similarity({0}, {1})",
                field, Expressions.constant(title)
        );
    }

    /**
     * 하버사인(acos) 기반 거리 (km)
     * acos 도메인 문제를 피하기 위해 least/greatest로 클램프
     */
    private static NumberExpression<Double> distanceKmExpr(
            NumberExpression<Double> lat1,
            NumberExpression<Double> lon1,
            double lat2, double lon2
    ) {
        return Expressions.numberTemplate(
                Double.class,
                "6371 * acos(least(1.0, greatest(-1.0, " +
                        "cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + " +
                        "sin(radians({0})) * sin(radians({1}))" +
                        ")))",
                Expressions.constant(lat2),
                lat1,
                lon1,
                Expressions.constant(lon2)
        );
    }
}
