package com.saecdo18.petmily.jwt;

import com.saecdo18.petmily.util.AuthenticationGetMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refresh")
@RequiredArgsConstructor
public class TokenController {

    private final AuthenticationGetMemberId authenticationGetMemberId;
    private final TokenProvider tokenProvider;
    @PostMapping
    public ResponseEntity<TokenDto> reIssuance(@RequestBody ReTokenDto reIssuanceTokenDto) {
        long memberId = authenticationGetMemberId.getMemberId();
        if(tokenProvider.extractMemberId(reIssuanceTokenDto.accessToken).get() != memberId)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        if(!tokenProvider.isTokenValid(reIssuanceTokenDto.refreshToken))
            return new ResponseEntity<>(TokenDto.builder().message("재로그인이 필요합니다.").build(), HttpStatus.OK);

        String accessToken = tokenProvider.createAccessToken(memberId);
        String refreshToken = tokenProvider.createRefreshToken();
        TokenDto tokenDto = TokenDto.builder().message("Access Token & RefreshToken 재발급 성공").accessToken(accessToken).refreshToken(refreshToken).build();

        return ResponseEntity.ok(tokenDto);
    }
}
