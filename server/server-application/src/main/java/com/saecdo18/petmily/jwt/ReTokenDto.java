package com.saecdo18.petmily.jwt;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class ReTokenDto {
    String accessToken;
    String refreshToken;
}
