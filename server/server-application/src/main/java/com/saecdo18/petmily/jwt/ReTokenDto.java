package com.saecdo18.petmily.jwt;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReTokenDto {
    String accessToken;
    String refreshToken;
}
