package com.wego.wego.domain.settlement.dto;

import java.util.List;

public record SettlementResultResponse (
    int budget,
    int people,
    List<Participant> participants
){
    public record Participant (
            String name,
            int paidTotal, // 실제로 낸 총액
            int share,     // 분배한 부담액(각 결제의 paidTotal/people 올림 몫 합계, 보정 반영)
            int diff       // 정산금 = share - paidTotal (양수: 추가로 내야 함, 음수: 받아야 함)
    ){
        public static Participant from(String name, int paidTotal, int share, int diff) {
            return new Participant(
                    name,
                    paidTotal,
                    share,
                    diff
            );
        }
    }

    public static SettlementResultResponse from(int budget, int people, List<Participant> participants) {
        return new SettlementResultResponse(
                budget,
                people,
                participants
        );
    }

}
