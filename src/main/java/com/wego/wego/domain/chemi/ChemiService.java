package com.wego.wego.domain.chemi;

import com.wego.wego.domain.chemi.dto.ChemiDto;
import com.wego.wego.domain.chemi.dto.ChemiLabelResponse;
import com.wego.wego.domain.chemi.dto.ChemiListResponse;
import com.wego.wego.domain.chemi.entity.Chemi;
import com.wego.wego.domain.chemi.repository.ChemiRepository;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChemiService {
    private final MemberRepository memberRepository;
    private final ChemiRepository chemiRepository;

    public String evaluateChemiResult(List<String> answers) {
        Map<String, Integer> scoreTable = ChemiMappingConstants.newScoreTable();

        for (int i = 1; i < answers.size()+1; i++) {
            String key = ChemiMappingConstants.CHEMI_MAP.get(i).get(answers.get(i-1));
//            log.info("key:{}",key);
            scoreTable.put(key, scoreTable.get(key) + 1);
        }

        return scoreTable.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }


    public List<ChemiDto> findAll() {

        return chemiRepository.findAll().stream()
                .map(c -> new ChemiDto(c.getName(),c.getImage(), c.getDescription()))
                .toList();

    }
    public List<ChemiLabelResponse> findAllLabels() {

        return chemiRepository.findAll().stream()
                .map(c -> new ChemiLabelResponse(c.getId(), c.getName(),c.getDescription()))
                .toList();

    }

    public ChemiListResponse findBySimilarChemi(Member member) {
        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없음"));

        Long chemiId = findMember.getChemiId();
        if (chemiId == null) {
            throw new RuntimeException("케미 테스트 해야 됨");
        }else {
            Chemi chemi = chemiRepository.findById(chemiId)
                    .orElseThrow(() -> new RuntimeException("케미 없음"));

            Set<Chemi> similarChemis = chemi.getSimilarChemis();
            return new ChemiListResponse(similarChemis.stream().map(
                    c -> new ChemiDto(c.getName(),c.getImage(),c.getDescription())).toList());
        }
    }
}
