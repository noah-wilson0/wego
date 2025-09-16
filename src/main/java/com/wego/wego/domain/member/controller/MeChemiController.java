package com.wego.wego.domain.member.controller;

import com.wego.wego.domain.chemi.dto.ChemiDto;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.service.MeChemiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequestMapping("/me")
@RestController
@RequiredArgsConstructor
public class MeChemiController {
    private final MeChemiService meChemiService;

    /**
     * 케미 유형 저장용 컨트롤러
     */
    @PostMapping("/chemi")
    public void chemiPersist(@RequestBody String chemi, @AuthenticationPrincipal Member member) {
        meChemiService.persistMemberChemi(chemi, member);
    }


    @GetMapping("/chemi")
    public ResponseEntity<ChemiDto> getMemberChemi(@AuthenticationPrincipal Member member) {
        ChemiDto chemiDto = meChemiService.findByMemberChemi(member);

        return ResponseEntity.ok(chemiDto);
    }
}
