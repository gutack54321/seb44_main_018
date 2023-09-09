package com.saecdo18.petmily.jwt;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReTokenDto {
    String accessToken;
    String refreshToken;
}
