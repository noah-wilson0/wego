package com.wego.wego.domain.member.service;

import com.wego.wego.domain.feed.dto.FeedResponse;
import com.wego.wego.domain.member.dto.MemberDetailResponse;
import com.wego.wego.domain.member.dto.UpdatePasswordRequest;
import com.wego.wego.domain.member.dto.UpdateUserInfoRequest;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class MeService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    public MemberDetailResponse getMemberDetail(long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new RuntimeException("존재 하지 않는 회원")
        );
        return new MemberDetailResponse(member.getUsername(), member.getName());
    }

    @Transactional
    public void updatePassword(String username, UpdatePasswordRequest updatePasswordRequest) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("존재 하지 않는 회원"));
        member.changePassword(passwordEncoder.encode(updatePasswordRequest.newPassword()));
    }

    @Transactional
    public void updateMemberInfo(UpdateUserInfoRequest updateUserInfoRequest, Member member) {
        Member findMember = memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new RuntimeException("존재 하지 않는 회원"));
        findMember.changeName(updateUserInfoRequest.name());
    }

    @Transactional
    public void deleteMember(Member member) {
        Member findMember = memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않은 회원"));
        memberRepository.deleteByUsername(findMember.getUsername());

    }
    /**
     * TODO 모든 Feed 일정 데이터가 필요한게 아니고 피드 카드에 표현될 정도만 필요한거라서 개선 필요함
     * @param member
     * @return
     */
    public List<FeedResponse> getFeedsByMember(Member member) {
        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(
                        () -> new RuntimeException("존재하지 않는 회원")
                );
        return findMember.getFeeds().stream().map(FeedResponse::from).toList();
    }

}
