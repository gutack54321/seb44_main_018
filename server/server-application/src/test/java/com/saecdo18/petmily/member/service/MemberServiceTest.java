package com.saecdo18.petmily.member.service;

import com.saecdo18.petmily.feed.entity.Feed;
import com.saecdo18.petmily.feed.entity.FeedImage;
import com.saecdo18.petmily.image.dto.ImageDto;
import com.saecdo18.petmily.image.entity.Image;
import com.saecdo18.petmily.member.dto.FollowMemberDto;
import com.saecdo18.petmily.member.dto.MemberDto;
import com.saecdo18.petmily.member.entity.FollowMember;
import com.saecdo18.petmily.member.entity.Member;
import com.saecdo18.petmily.member.mapper.FollowMemberMapper;
import com.saecdo18.petmily.member.mapper.MemberMapper;
import com.saecdo18.petmily.member.repository.FollowMemberRepository;
import com.saecdo18.petmily.member.repository.MemberRepository;
import com.saecdo18.petmily.pet.dto.PetDto;
import com.saecdo18.petmily.pet.entity.Pet;
import com.saecdo18.petmily.pet.entity.PetImage;
import com.saecdo18.petmily.pet.mapper.PetMapper;
import com.saecdo18.petmily.pet.repository.PetImageRepository;
import com.saecdo18.petmily.pet.repository.PetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PetImageRepository petImageRepository;

    @Mock
    private PetMapper petMapper;

    @Mock
    private FollowMemberRepository followMemberRepository;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private FollowMemberMapper followMemberMapper;

    @Test
    @DisplayName("회원 조회 성공: follow 사용자2 -> 사용자1")
    void getMemberSuccessFollow() {


        long hostMemberId = 1L;
        long guestMemberId = 2L;

        Member hostMember = new Member();
        ReflectionTestUtils.setField(hostMember, "memberId", hostMemberId);

        Pet pet = new Pet();
        ReflectionTestUtils.setField(pet, "petId", 1L);
        ReflectionTestUtils.setField(pet, "name", "메시");
        ReflectionTestUtils.setField(pet, "member", hostMember);

        List<Pet> petList = List.of(pet);

        PetDto.Response petResponse = PetDto.Response.builder()
                .petId(1l)
                .name("메시")
                .build();

        Image image = Image.builder().originalFilename("image.jpg").uploadFileURL("http://image.jpg").build();

        PetImage petImage = new PetImage();
        ReflectionTestUtils.setField(petImage, "pet", pet);
        ReflectionTestUtils.setField(petImage, "image", image);

        ImageDto imageDto = ImageDto.builder()
                .imageId(1L)
                .originalFilename("image.jpg")
                .uploadFileURL("http://image.jpg")
                .build();

        FollowMember followMember = new FollowMember();   //게스트가 팔로우 중
        ReflectionTestUtils.setField(followMember, "followerMember", hostMember);
        ReflectionTestUtils.setField(followMember, "followingId", guestMemberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(hostMember));
        given(petRepository.findByMember(Mockito.any(Member.class))).willReturn(petList);
        given(petMapper.petToPetResponseDto(Mockito.any(Pet.class))).willReturn(petResponse);
        given(petImageRepository.findByPet(Mockito.any(Pet.class))).willReturn(petImage);
        given(petMapper.imageToImageDto(Mockito.any(Image.class))).willReturn(imageDto);
        given(followMemberRepository.findByFollowerMemberAndFollowingId(Mockito.any(Member.class), Mockito.anyLong())).willReturn(Optional.of(followMember));
        given(memberMapper.memberToMemberResponseDto(Mockito.any(Member.class))).willReturn(memberResponse);
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(hostMember));

        // when
        MemberDto.Response result = memberService.getMember(hostMemberId, guestMemberId);


        verify(memberRepository, times(2)).findById(hostMemberId);
        verify(petRepository, times(1)).findByMember(hostMember);

        assertEquals(result.isGuestFollow(), true); //팔로우 상태
        assertEquals(result.getMemberInfo().getMemberId(), hostMemberId); //호스트 회원 아이디
        assertEquals(result.getPets().size(), 1);  //반려동물 리스트 크기
        assertEquals(result.getPets().get(0).getName(), "메시");  //반려동물 중 첫번째 반려동물 이름

    }

    @Test
    @DisplayName("회원 조회 성공: unfollow 사용자2 -> 사용자1")
    void getMemberSuccessUnfollow() {


        long hostMemberId = 1L;
        long guestMemberId = 2L;

        Member hostMember = new Member();
        ReflectionTestUtils.setField(hostMember, "memberId", hostMemberId);

        Pet pet = new Pet();
        ReflectionTestUtils.setField(pet, "petId", 1L);
        ReflectionTestUtils.setField(pet, "name", "메시");
        ReflectionTestUtils.setField(pet, "member", hostMember);

        List<Pet> petList = List.of(pet);

        PetDto.Response petResponse = PetDto.Response.builder()
                .petId(1l)
                .name("메시")
                .build();

        Image image = Image.builder().originalFilename("image.jpg").uploadFileURL("http://image.jpg").build();

        PetImage petImage = new PetImage();
        ReflectionTestUtils.setField(petImage, "pet", pet);
        ReflectionTestUtils.setField(petImage, "image", image);

        ImageDto imageDto = ImageDto.builder()
                .imageId(1L)
                .originalFilename("image.jpg")
                .uploadFileURL("http://image.jpg")
                .build();

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(hostMember));
        given(petRepository.findByMember(Mockito.any(Member.class))).willReturn(petList);
        given(petMapper.petToPetResponseDto(Mockito.any(Pet.class))).willReturn(petResponse);
        given(petImageRepository.findByPet(Mockito.any(Pet.class))).willReturn(petImage);
        given(petMapper.imageToImageDto(Mockito.any(Image.class))).willReturn(imageDto);
        given(followMemberRepository.findByFollowerMemberAndFollowingId(Mockito.any(Member.class), Mockito.anyLong())).willReturn(Optional.empty()); //게스트가 호스트 팔로우하진 않은 상태
        given(memberMapper.memberToMemberResponseDto(Mockito.any(Member.class))).willReturn(memberResponse);
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(hostMember));

        // when
        MemberDto.Response result = memberService.getMember(hostMemberId, guestMemberId);


        verify(memberRepository, times(2)).findById(hostMemberId);
        verify(petRepository, times(1)).findByMember(hostMember);

        assertEquals(result.isGuestFollow(), false);  //게스트 팔로우 상태
        assertEquals(result.getMemberInfo().getMemberId(), hostMemberId); //호스트 회원 아이디
        assertEquals(result.getMemberInfo().getMemberId(), 1L); //호스트 회원 아이디
    }

    @Test
    @DisplayName("회원 조회 성공 : 찾는 반려동물이 없어서 빈리스트로 조회")
    void getMemberSuccessNonePet() {

        long hostMemberId = 1L;
        long guestMemberId = 2L;

        Member hostMember = new Member();
        ReflectionTestUtils.setField(hostMember, "memberId", hostMemberId);

        List<Pet> petList = new ArrayList<>();


        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", hostMember);
        ReflectionTestUtils.setField(followMember, "followingId", guestMemberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(hostMember));
        given(petRepository.findByMember(Mockito.any(Member.class))).willReturn(petList);

        given(followMemberRepository.findByFollowerMemberAndFollowingId(Mockito.any(Member.class), Mockito.anyLong())).willReturn(Optional.of(followMember));
        given(memberMapper.memberToMemberResponseDto(Mockito.any(Member.class))).willReturn(memberResponse);
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(hostMember));

        // when
        MemberDto.Response result = memberService.getMember(hostMemberId, guestMemberId);


        verify(memberRepository, times(2)).findById(hostMemberId);
        verify(petRepository, times(1)).findByMember(hostMember);

        assertEquals(result.getMemberInfo().getMemberId(), hostMemberId); //호스트 회원 아이디
        assertEquals(result.getPets().size(), 0);  //반려동물 없을 경우 빈리스트 출력

    }

    @Test
    @DisplayName("회원 조회 실패 : 찾는 멤버가 없어서 생기는 예외")
    void getMemberNoneHostMember() {


        long hostMemberId = 1L;
        long guestMemberId = 2L;

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class , () -> memberService.getMember(hostMemberId, guestMemberId));

        assertEquals(exception.getMessage(), "수정할 멤버가 없습니다");
    }

    @Test
    @DisplayName("회원 수정 성공")
    void updateMemberStatusSuccess() {

        long memberId = 1L;
        String exNickname="수정하기전닉네임";
        String exAddress="서울시 강서구 수정동";
        String nickname = "수정된 닉네임";
        String address = "서울시 강서구 마곡동";

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "nickname", exNickname);
        ReflectionTestUtils.setField(member, "address", exAddress);

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .nickname(nickname)
                .build();

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .address(address)
                .memberInfo(memberInfo)
                .build();




        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member)); // 처음 존재하는 회원인지 찾음
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member)); //회원을 memberDto.Info로 변환할 때 다시 회원인지 찾음
        given(memberMapper.memberToMemberResponseDto(Mockito.any(Member.class))).willReturn(memberResponse);  //엔티티를 디티오로 변환


        // when
        MemberDto.Response result = memberService.updateMemberStatus(memberId, nickname, address);


        verify(memberRepository, times(2)).findById(memberId);

        assertEquals(result.getMemberInfo().getMemberId(), memberId); //수정한 회원아이디
        assertEquals(result.getAddress(), address); //수정된 주소지
        assertEquals(result.getMemberInfo().getNickname(), nickname); //수정된 주소지
    }

    @Test
    @DisplayName("회원 수정 실패 : 찾는 멤버가 없어서 생기는 예외")
    void updateMemberStatusNoneMember() {

        long memberId = 1L;
        String nickname = "닉네임";
        String address = "서울시 강서구 마곡동";

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();


        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class , () -> memberService.updateMemberStatus(memberId, nickname, address));

        assertEquals(exception.getMessage(), "수정할 멤버가 없습니다");

    }

    @Test
    @DisplayName("팔로잉 신청 성공: follow 사용자2 -> 사용자1")
    void followMemberSuccess() {

        long memberId = 1L;

        long followingId = 2L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();

        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", member);
        ReflectionTestUtils.setField(followMember, "followingId", followingId);

        FollowMemberDto.Response followMemberResponse = FollowMemberDto.Response.builder()
                .memberInfo(memberInfo)
                .build();


        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(followMemberRepository.findByFollowerMemberAndFollowingId(Mockito.any(Member.class), Mockito.anyLong())).willReturn(Optional.empty()); //팔로우테이블에 없다면 팔로우 진행
        given(followMemberMapper.followMemberToFollowMemberResponseDto(Mockito.any(FollowMember.class))).willReturn(followMemberResponse);


        // when
        FollowMemberDto.Response result = memberService.followMember(memberId, followingId);


        verify(memberRepository, times(2)).findById(memberId);

        assertEquals(result.getMemberInfo().getMemberId(), memberId); //팔로잉 된 follower 회원 아이디
        assertEquals(result.isFollow(), true);  // follow 상태
    }

    @Test
    @DisplayName("언팔로잉 신청 성공: follow 사용자2 -> 사용자1")
    void nuFollowMemberSuccess() {

        long memberId = 1L;

        long followingId = 2L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();

        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", member);
        ReflectionTestUtils.setField(followMember, "followingId", followingId);

        FollowMemberDto.Response followMemberResponse = FollowMemberDto.Response.builder()
                .memberInfo(memberInfo)
                .build();


        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(followMemberRepository.findByFollowerMemberAndFollowingId(Mockito.any(Member.class), Mockito.anyLong())).willReturn(Optional.of(followMember)); //팔로우테이블에 있다면 언팔로우 진행
        given(followMemberMapper.followMemberToFollowMemberResponseDto(Mockito.any(FollowMember.class))).willReturn(followMemberResponse);


        // when
        FollowMemberDto.Response result = memberService.followMember(memberId, followingId);


        verify(memberRepository, times(2)).findById(memberId);

        assertEquals(result.getMemberInfo().getMemberId(), memberId); //언팔로우 된 follower 회원 아이디
        assertEquals(result.isFollow(), false); // follow 상태
    }

    @Test
    @DisplayName("팔로잉 신청 실패 : 찾는 멤버가 없어서 생기는 예외")
    void followMemberNoneMember() {

        long memberId = 1L;

        long followingId = 2L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();

        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", member);
        ReflectionTestUtils.setField(followMember, "followingId", followingId);

        FollowMemberDto.Response followMemberResponse = FollowMemberDto.Response.builder()
                .memberInfo(memberInfo)
                .build();


        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class , () -> memberService.followMember(memberId, followingId));

        assertEquals(exception.getMessage(), "팔로우 할 팔로워를 찾지 못했습니다");

    }

    @Test
    @DisplayName("팔로잉 리스트 가져오기 성공")
    void followListSuccess() {

        long memberId = 1L;

        long followingId = 2L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();

        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", member);
        ReflectionTestUtils.setField(followMember, "followingId", followingId);

        List<FollowMember> followMemberList = List.of(followMember);

        FollowMemberDto.Response followMemberResponse = FollowMemberDto.Response.builder()
                .memberInfo(memberInfo)
                .build();


        given(followMemberRepository.findByFollowingId(Mockito.anyLong())).willReturn(Optional.of(followMemberList));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(followMemberMapper.followMemberToFollowMemberResponseDto(Mockito.any(FollowMember.class))).willReturn(followMemberResponse);

        // when
        List<FollowMemberDto.Response> result = memberService.followList(followingId);

        assertEquals(result.get(0).getMemberInfo().getMemberId(), memberId); //followingId가 팔로우한 멤버 아이디
    }

    @Test
    @DisplayName("팔로잉 리스트 가져오기 성공: 팔로잉한게 없어서 빈리스트로 출력")
    void followListEmptySuccess() {

        long followingId = 2L;

        List<FollowMember> followMemberList = new ArrayList<>();

        given(followMemberRepository.findByFollowingId(Mockito.anyLong())).willReturn(Optional.of(followMemberList));

        // when
        List<FollowMemberDto.Response> result = memberService.followList(followingId);

        assertEquals(result.size(), 0); //followingId가 팔로우한 멤버 아이디
    }

    @Test
    @DisplayName("팔로잉 리스트 가져오기 실패 : 찾는 멤버가 없어서 생기는 예외")
    void followListNoneMember() {

        long memberId = 1L;

        long followingId = 2L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();

        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", member);
        ReflectionTestUtils.setField(followMember, "followingId", followingId);

        List<FollowMember> followMemberList = List.of(followMember);

        FollowMemberDto.Response followMemberResponse = FollowMemberDto.Response.builder()
                .memberInfo(memberInfo)
                .build();


        given(followMemberRepository.findByFollowingId(Mockito.anyLong())).willReturn(Optional.of(followMemberList));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class , () -> memberService.followList(followingId));

        assertEquals(exception.getMessage(), "사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("회원의 이미지 변경 성공(자신의 반려동물 중 선택하면 반려동물의 프로필로 회원의 이미지가 바뀜)")
    void changeImageSuccess() {

        long memberId = 1L;
        long petId = 1L;
        long followingId = 2L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Image image = new Image();
        ReflectionTestUtils.setField(image, "imageId", 1L);
        ReflectionTestUtils.setField(image, "originalFilename", "xmrqufgkstkwls.png");
        ReflectionTestUtils.setField(image, "uploadFileURL", "http://adfjeiiv.dkjfibj");

        PetImage petImage = new PetImage();
        ReflectionTestUtils.setField(petImage, "PetImageId", 1l);
        ReflectionTestUtils.setField(petImage, "image", image);

        Pet pet = new Pet();
        ReflectionTestUtils.setField(pet, "petId", 1L);
        ReflectionTestUtils.setField(pet, "name", "메시");
        ReflectionTestUtils.setField(pet, "member", member);
        ReflectionTestUtils.setField(pet, "petImage", petImage);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();


        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", member);
        ReflectionTestUtils.setField(followMember, "followingId", followingId);

        List<FollowMember> followMemberList = List.of(followMember);

        FollowMemberDto.Response followMemberResponse = FollowMemberDto.Response.builder()
                .memberInfo(memberInfo)
                .build();

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(petRepository.findById(Mockito.anyLong())).willReturn(Optional.of(pet));
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(member);


        // when
        memberService.changeImage(memberId, petId);
    }

    @Test
    @DisplayName("회원의 이미지 변경 실패(자신의 반려동물 중 선택하면 반려동물의 프로필로 회원의 이미지가 바뀜) : 찾는 멤버가 없어서 생기는 예외")
    void changeImageNoneMember() {

        long memberId = 1L;
        long petId = 1L;
        long followingId = 2L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Image image = new Image();
        ReflectionTestUtils.setField(image, "imageId", 1L);
        ReflectionTestUtils.setField(image, "originalFilename", "xmrqufgkstkwls.png");
        ReflectionTestUtils.setField(image, "uploadFileURL", "http://adfjeiiv.dkjfibj");

        PetImage petImage = new PetImage();
        ReflectionTestUtils.setField(petImage, "PetImageId", 1l);
        ReflectionTestUtils.setField(petImage, "image", image);

        Pet pet = new Pet();
        ReflectionTestUtils.setField(pet, "petId", 1L);
        ReflectionTestUtils.setField(pet, "name", "메시");
        ReflectionTestUtils.setField(pet, "member", member);
        ReflectionTestUtils.setField(pet, "petImage", petImage);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();


        FollowMember followMember = new FollowMember();
        ReflectionTestUtils.setField(followMember, "followerMember", member);
        ReflectionTestUtils.setField(followMember, "followingId", followingId);

        List<FollowMember> followMemberList = List.of(followMember);

        FollowMemberDto.Response followMemberResponse = FollowMemberDto.Response.builder()
                .memberInfo(memberInfo)
                .build();

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> memberService.changeImage(memberId, petId));

        assertEquals(exception.getMessage(), "수정할 멤버가 없습니다");

    }

    @Test
    @DisplayName("닉네임이 중복되지 않아 사용이 가능한 경우")
    void checkNicknameCanUse() {
        long memberId = 1L;
        String nickname= "testnick";

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);


        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();


        given(memberRepository.findByNickname(Mockito.anyString())).willReturn(Optional.of(member));



        // when
        memberService.checkNickname(nickname);
    }

    @Test
    @DisplayName("닉네임이 중복되어 사용이 불가능한 경우")
    void checkNicknameCantUse() {
        long memberId = 1L;
        String nickname= "testnick";

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        MemberDto.Response memberResponse = MemberDto.Response.builder()
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(1L)
                .build();

        given(memberRepository.findByNickname(Mockito.anyString())).willReturn(Optional.empty());

        // when
        memberService.checkNickname(nickname);
    }
}