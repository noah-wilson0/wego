package com.wego.wego.domain.member.repository;

import com.wego.wego.domain.plan.dto.draft.auto.ChemiSummaryForAiDto;
import com.wego.wego.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);
    void deleteByUsername(String username);


    @Query("""
    select new com.wego.wego.domain.plan.dto.draft.auto.ChemiSummaryForAiDto(
        c.name,
        c.description
    )
    from Member m
     join Chemi c on(m.id = :memberId)
    where m.chemiId = c.id
    """)
    Optional<ChemiSummaryForAiDto> findChemiSummaryForAiDtoByMemberId(@Param("memberId")Long memberId);
}
