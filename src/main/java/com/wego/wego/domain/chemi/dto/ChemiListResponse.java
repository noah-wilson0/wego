package com.wego.wego.domain.chemi.dto;

import java.util.List;

public record ChemiListResponse<T>(
        List<T> chemiDtoList
){}
