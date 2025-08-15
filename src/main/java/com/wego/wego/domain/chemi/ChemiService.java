package com.wego.wego.domain.chemi;

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
    @Transactional
    public void persistMemberChemi(String chemi, Member member) {
        log.info(chemi);
        Chemi findChemi = chemiRepository.findByName(chemi)
                .orElseThrow(() -> {
                    throw new RuntimeException("케미 못참음");
                });

        Member findMember = memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> {
                    throw new RuntimeException("유저 못참음");
                });

        findMember.updateChemiId(findChemi.getId());
    }
}
