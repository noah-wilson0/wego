package com.wego.wego.domain.chemi.controller;

import com.wego.wego.domain.chemi.ChemiService;
import com.wego.wego.domain.chemi.dto.ChemiDto;
import com.wego.wego.domain.chemi.dto.ChemiListResponse;
import com.wego.wego.domain.chemi.dto.ChemiRequest;
import com.wego.wego.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chemis")
@RequiredArgsConstructor
public class ChemiController {
    private static final Logger log = LoggerFactory.getLogger(ChemiController.class);
    private final ChemiService chemiService;

    /**
     * 일반 적인 상황에서 케미 데이터 리스트
     * @return
     */
    @GetMapping("/all")
    public ResponseEntity<ChemiListResponse> getChemiList() {
        ChemiListResponse chemiListResponse = new ChemiListResponse(chemiService.findAll());

        return ResponseEntity.ok(chemiListResponse);
    }

    /**
     * 케미 유형 테스트 결과 반환 컨트롤러
     *  케미 테스트는 localStroge에 저장 후 마지막에만 요청
     * {
     * 	answer: [1,3,2,1,3,2,2,1,3,3,3,2]
     * }
     * 피그마 설계는 이렇게 했었음
     *
     * 매핑 테이블 만들기
     * {1:{1:"푸트 파이터", 2:"솔로 여행가", 3:"슬로우 트레블러"},
     * 2:{1:"릴렉서러", 2:"페스타 러버", 3:"푸드 파이터"}}
     * answer에 대해서 매핑 테이블을 통해 선택지의 케미 유형을 찾음
     *
     * 케미 테이블 만들기
     * MAP<String, int></>{"푸트 파이터":0, "솔로 여행가":0, "슬로우 트레블러":0}
     *
     * 매핑 테이블을 통해 찾은 케미 유형을 통해 케미 테이블의 score +1 올리기
     * @param chemiRequest
     */
    @PostMapping("/result")
    public ResponseEntity<String> chemiResult(@RequestBody ChemiRequest chemiRequest) {
        String chemiResult = chemiService.evaluateChemiResult(chemiRequest.answers());
        log.info("케미 결과:{}",chemiResult);
        return ResponseEntity.ok(chemiResult);

    }


    @GetMapping("/similar")
    public ResponseEntity<ChemiListResponse> getChemiSimilar(@AuthenticationPrincipal Member member) {
        ChemiListResponse chemiListResponse = chemiService.findBySimilarChemi(member);

        return ResponseEntity.ok(chemiListResponse);
    }

    /**
     * 피드 생성 시 케미 테그 리스트 데이터
     * @return
     */
    @GetMapping("/labels")
    public ResponseEntity<ChemiListResponse> getChemiLabels() {
        ChemiListResponse chemiListResponse = new ChemiListResponse(chemiService.findAllLabels());

        return ResponseEntity.ok(chemiListResponse);
    }
}
