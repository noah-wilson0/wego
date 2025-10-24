package com.wego.wego.domain.placeStorage.repository;

import com.querydsl.core.annotations.QueryProjection;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wego.wego.domain.placeStorage.dto.PlaceStorageItemDto;
import com.wego.wego.domain.placeStorage.entity.QPlaceStorage;
import com.wego.wego.external.tourapi.place.entity.QPlace;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.wego.wego.domain.placeStorage.entity.QPlaceStorage.placeStorage;
import static com.wego.wego.external.tourapi.place.entity.QPlace.place;

public class PlaceStorageQueryRepositoryImpl implements PlaceStorageQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public PlaceStorageQueryRepositoryImpl(EntityManager em) {
        this.jpaQueryFactory = new JPAQueryFactory(em);
    }
    @Override
    public Page<PlaceStorageItemDto> findStorageItems(Long memberId, Long travelPlanId, Pageable pageable) {
        List<PlaceStorageItemDto> placeStorageItems = jpaQueryFactory.select(Projections.constructor(PlaceStorageItemDto.class,
                        placeStorage.sequence,
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
                .from(placeStorage)
                .join(placeStorage.place, place)
                .where(
                        placeStorage.member.id.eq(memberId),
                        placeStorage.travelPlan.id.eq(travelPlanId)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(placeStorage.sequence.asc())
                .fetch();

        JPAQuery<Long> count = jpaQueryFactory
                .select(placeStorage.count())
                .from(placeStorage)
                .where(
                        placeStorage.member.id.eq(memberId),
                        placeStorage.travelPlan.id.eq(travelPlanId)
                );


        return PageableExecutionUtils.getPage(placeStorageItems,pageable,count::fetchCount);
    }

//    @Override
//    public Page<PlaceStorageItemDto> findStorageItems(Long memberId, Long travelPlanId, Pageable pageable) {
//        List<PlaceStorageItemDto> placeStorageItems = jpaQueryFactory.select(Projections.constructor(PlaceStorageItemDto.class,
//                        placeStorage.sequence,
//                        place
//
//                ))
//                .from(placeStorage)
//                .join(placeStorage.place, place).fetchJoin()
//                .where(
//                        placeStorage.member.id.eq(memberId),
//                        placeStorage.travelPlan.id.eq(travelPlanId)
//                )
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .orderBy(placeStorage.sequence.asc())
//                .fetch();
//
//        JPAQuery<Long> count = jpaQueryFactory
//                .select(placeStorage.count())
//                .from(placeStorage)
//                .where(
//                        placeStorage.member.id.eq(memberId),
//                        placeStorage.travelPlan.id.eq(travelPlanId)
//                );
//
//
//        return PageableExecutionUtils.getPage(placeStorageItems,pageable,count::fetchCount);
//    }


    private static NumberExpression<Double> toDoubleOrZero(StringPath path) {
        // HQL 표준: cast(nullif(...,'') as double), null이면 0.0
        return Expressions.numberTemplate(
                Double.class,
                "coalesce(cast(nullif({0}, '') as double), 0.0)",
                path
        );
    }
}
