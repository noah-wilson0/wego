package com.wego.wego.domain.placeStorage.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.placeStorage.dto.PlaceStorageItemDto;
import com.wego.wego.domain.placeStorage.service.PlaceStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/travel-plans")
@RestController
@RequiredArgsConstructor
public class PlaceStorageController {
    private final PlaceStorageService placeStorageService;

    @GetMapping("/{travelPlanId}/storages")
    public ResponseEntity<Page<PlaceStorageItemDto>> getStorages(
            @PathVariable("travelPlanId") String travelPlanId,
            @AuthenticationPrincipal Member member,
            @PageableDefault(size=20) Pageable pageable) {
        Page<PlaceStorageItemDto> placeStorages = placeStorageService.findAll(member.getId(), Long.valueOf(travelPlanId), pageable);
        return ResponseEntity.ok(placeStorages);
    }

    @PostMapping("/{travelPlanId}/storages")
    public ResponseEntity<?> persistStorages(
            @PathVariable("travelPlanId") String travelPlanId,
            @AuthenticationPrincipal Member member,
            @RequestBody List<String> placeContentIds) {
        placeStorageService.saveAll(member.getId(), Long.valueOf(travelPlanId), placeContentIds);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{travelPlanId}/storages")
    public ResponseEntity<?> updateStorages(
            @PathVariable("travelPlanId") String travelPlanId,
            @AuthenticationPrincipal Member member,
            @RequestBody List<String> placeContentIds) {
        placeStorageService.replaceAll(member.getId(), Long.valueOf(travelPlanId), placeContentIds);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/{travelPlanId}/storages")
    public ResponseEntity<?> deleteStorages(
            @PathVariable("travelPlanId") String travelPlanId,
            @AuthenticationPrincipal Member member) {
        placeStorageService.delete(member.getId(), Long.valueOf(travelPlanId));
        return ResponseEntity.ok().build();
    }


}
