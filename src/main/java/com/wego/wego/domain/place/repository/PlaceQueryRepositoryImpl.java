package com.wego.wego.domain.place.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
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

}
