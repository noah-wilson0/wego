package com.wego.wego.domain.placeStorage.service;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.domain.placeStorage.dto.PlaceStorageItemDto;
import com.wego.wego.domain.placeStorage.entity.PlaceStorage;
import com.wego.wego.domain.placeStorage.entity.PlaceStorageId;
import com.wego.wego.domain.placeStorage.repository.PlaceStorageRepository;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceStorageService {
    private final PlaceStorageRepository placeStorageRepository;

    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final TravelPlanRepository travelPlanRepository;

    public Page<PlaceStorageItemDto> findAll(Long memberId, Long travelPlanId, Pageable pageable) {
        return placeStorageRepository.findStorageItems(memberId, travelPlanId, pageable);
    }

    @Transactional
    public void saveAll(Long memberId, Long travelPlanId, List<String> placeContentIds) {

        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 회원"));
        TravelPlan findTravelPlan = travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 여행 일정"));

        List<PlaceStorage> placeStorages=new ArrayList<>();
        for (int i = 0; i < placeContentIds.size(); i++) {

            Place findPlace = placeRepository.findByContentId(placeContentIds.get(i))
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 장소"));

            placeStorages.add(
                    PlaceStorage.builder()
                            .id(
                                    new PlaceStorageId(
                                            findMember.getId(),
                                            findTravelPlan.getId(),
                                            findPlace.getId()
                                    )
                            )
                            .member(findMember)
                            .travelPlan(findTravelPlan)
                            .place(findPlace)
                            .sequence(i+1)
                            .build()
            );
        }

        placeStorageRepository.saveAll(placeStorages);
    }

    @Transactional
    public void replaceAll(Long memberId, Long travelPlanId, List<String> placeContentIds) {
        placeStorageRepository.deleteByMemberIdAndTravelPlanId(memberId, travelPlanId);
        saveAll(memberId, travelPlanId, placeContentIds);
    }

    @Transactional
    public void delete(Long memberId, Long travelPlanId) {
        placeStorageRepository.deleteByMemberIdAndTravelPlanId(memberId, travelPlanId);
    }
}
