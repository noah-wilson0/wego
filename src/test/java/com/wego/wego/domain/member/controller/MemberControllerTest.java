package com.wego.wego.domain.member.controller;
import com.wego.wego.domain.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Slf4j
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
//@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void signup() throws Exception {
        String username = "testuser1";
        String password = "pass1234!";
        String name = "테스터";

        String body = """
            { "username": "%s", "password": "%s", "name": "%s" }
            """.formatted(username, password, name);

        // 1) 회원가입 요청
        mockMvc.perform(post("/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 2) DB 저장 확인
        assertThat(memberRepository.findByUsername(username)).isPresent();
    }

    @Test
    void signIn() {
    }
}