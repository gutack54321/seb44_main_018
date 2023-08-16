package com.saecdo18.petmily.feed.controller;

import com.google.gson.Gson;
import com.saecdo18.petmily.feed.dto.FeedCommentDto;
import com.saecdo18.petmily.feed.dto.FeedDto;
import com.saecdo18.petmily.feed.dto.FeedDtoList;
import com.saecdo18.petmily.feed.dto.FeedServiceDto;
import com.saecdo18.petmily.feed.service.FeedServiceImpl;
import com.saecdo18.petmily.image.dto.ImageDto;
import com.saecdo18.petmily.jwt.TokenProvider;
import com.saecdo18.petmily.member.dto.MemberDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.NestedServletException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.config.name=application-test", "spring.config.location=classpath:/"})
@AutoConfigureMockMvc
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedServiceImpl feedService;

    @Autowired
    private Gson gson;

    @Autowired
    TokenProvider tokenProvider;

    @Test
    @DisplayName("피드 가져오기 - 성공")
    void getFeed() throws Exception {
        long feedId = 1L;
        long memberId = 1L;

        FeedDto.Response feedDto = getOneFeed(feedId);
        String content = gson.toJson(feedDto);

        given(feedService.changeFeedToFeedDtoResponse(Mockito.anyLong(), Mockito.anyLong())).willReturn(feedDto);

        ResultActions actions = mockMvc.perform(
                get("/feeds/{feed-id}", feedId)
                        .header("Authorization", tokenProvider.createAccessToken(memberId))
        );

        actions
                .andExpect(status().isOk())
                .andExpect(content().json(content))
                .andExpect(jsonPath("$.feedId").value(feedId))
                .andExpect(jsonPath("$.memberInfo.memberId").value(memberId));
    }

    @Test
    @DisplayName("피드 가져오기 - 실패: 잘못된 피드 아이디")
    void getFeed_Failure_InvalidFeedId() throws Exception {
        long invalidFeedId = -1L;
        long memberId = 1L;

        given(feedService.changeFeedToFeedDtoResponse(Mockito.anyLong(), Mockito.anyLong())).willThrow(new RuntimeException("피드를 찾을 수 없습니다."));

        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(
                    get("/feeds/{feed-id}", invalidFeedId)
                            .header("Authorization", tokenProvider.createAccessToken(memberId))
            );
        });

    }



//    @Test
//    @DisplayName("피드 최신순 가져오기")
//    void getFeedsRandom() throws Exception {
//        long memberId = 1L;
//        FeedDtoList feedDtoList = getFeedList(2);
//        FeedServiceDto.PreviousListIds previousListIds = getPreviousListIds();
//        String content = gson.toJson(feedDtoList);
//        String previousList = gson.toJson(previousListIds);
//
//        given(feedService.getFeedsRecent(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt())).willReturn(feedDtoList);
//
//        ResultActions actions = mockMvc.perform(
//                post("/feeds/all/list/random")
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .header("Authorization", tokenProvider.createAccessToken(memberId))
//                        .content(previousList)
//        );
//
//        actions
//                .andExpect(status().isOk())
//                .andExpect(content().json(content))
//                .andExpect(jsonPath("$.responseList[0].feedId").value(1))
//                .andExpect(jsonPath("$.responseList[1].feedId").value(2))
//                .andExpect(jsonPath("$.responseList[0].memberInfo.memberId").value(1))
//                .andExpect(jsonPath("$.responseList[1].memberInfo.memberId").value(1));
//    }

    @Test
    @DisplayName("사용자 피드 리스트 가져오기 - 성공: 피드가 있을 경우")
    void getFeeds() throws Exception {
        long memberId = 1L;
        int page = 0;
        int size = 10;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = FeedServiceDto.FeedListToServiceDto.builder().build();

        FeedDtoList feedDtoList = getFeedList(2);
        String content = gson.toJson(feedDtoList);

        given(feedService.getFeedsByMember(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong())).willReturn(feedListToServiceDto);
        given(feedService.changeFeedListToFeedResponseDto(Mockito.any(FeedServiceDto.FeedListToServiceDto.class), Mockito.anyLong())).willReturn(feedDtoList);

        mockMvc.perform(
                get("/feeds/my-feed")
                        .params(params)
                        .header("Authorization", tokenProvider.createAccessToken(memberId))
        ).andExpect(status().isOk())
                .andExpect(content().json(content))
                .andExpect(jsonPath("$.responseList[0].feedId").value(1))
                .andExpect(jsonPath("$.responseList[1].feedId").value(2))
                .andExpect(jsonPath("$.responseList[0].memberInfo.memberId").value(1))
                .andExpect(jsonPath("$.responseList[1].memberInfo.memberId").value(1));
    }

    @Test
    @DisplayName("사용자 피드 리스트 가져오기 - 성공: 피드가 없을 경우")
    void get_Empty_Feeds() throws Exception {
        long memberId = 1L;
        int page = 0;
        int size = 10;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = FeedServiceDto.FeedListToServiceDto.builder().build();

        FeedDtoList feedDtoList = FeedDtoList.builder()
                .responseList(Collections.emptyList())
                .build();
        String content = gson.toJson(feedDtoList);

        given(feedService.getFeedsByMember(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong())).willReturn(feedListToServiceDto);
        given(feedService.changeFeedListToFeedResponseDto(Mockito.any(FeedServiceDto.FeedListToServiceDto.class), Mockito.anyLong())).willReturn(feedDtoList);

        mockMvc.perform(
                        get("/feeds/my-feed")
                                .params(params)
                                .header("Authorization", tokenProvider.createAccessToken(memberId))
                ).andExpect(status().isOk())
                .andExpect(content().json(content));
    }

    @Test
    @DisplayName("타인 피드 리스트 가져오기 - 성공")
    void getFeedsByMember() throws Exception {
        long memberId = 1L;
        int page = 0, size = 10;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));
        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = FeedServiceDto.FeedListToServiceDto.builder().build();
        FeedDtoList feedDtoList = getFeedList(2);
        String content = gson.toJson(feedDtoList);

        given(feedService.getFeedsByMember(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong())).willReturn(feedListToServiceDto);
        given(feedService.changeFeedListToFeedResponseDto(Mockito.any(FeedServiceDto.FeedListToServiceDto.class), Mockito.anyLong())).willReturn(feedDtoList);

        mockMvc.perform(
                        get("/feeds/other-feed/{member-id}", memberId)
                                .params(params)
                                .header("Authorization", tokenProvider.createAccessToken(memberId))
                ).andExpect(status().isOk())
                .andExpect(content().json(content))
                .andExpect(jsonPath("$.responseList[0].feedId").value(1))
                .andExpect(jsonPath("$.responseList[1].feedId").value(2))
                .andExpect(jsonPath("$.responseList[0].memberInfo.memberId").value(1))
                .andExpect(jsonPath("$.responseList[1].memberInfo.memberId").value(1));
    }

    @Test
    @DisplayName("타인 피드 리스트 가져오기 - 실패: 잘못된 사용자 아이디")
    void getFeedsByMember_Failure_Incorrect_Member_Id() throws Exception {
        long memberId = 1L;
        int page = 0, size = 10;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));

        given(feedService.getFeedsByMember(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyLong())).willThrow(new RuntimeException("사용자를 찾을 수 없습니다."));

        assertThrows(NestedServletException.class, () -> {
            mockMvc.perform(
                    get("/feeds/other-feed/{member-id}", -1L)
                            .params(params)
                            .header("Authorization", tokenProvider.createAccessToken(memberId))
            );
        });
    }

    @Test
    @DisplayName("팔로우한 사용자 피드 리스트 가져오기 - 성공: 레디스 삭제 없음")
    void getFeedsByMemberFollow_No_Delete_Redis() throws Exception {
        long memberId = 1L;
        int page = 0, size = 10;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));
        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = FeedServiceDto.FeedListToServiceDto.builder().build();

        FeedDtoList feedDtoList = getFeedList(10);
        String content = gson.toJson(feedDtoList);

        given(feedService.getFeedsByMemberFollow(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt())).willReturn(feedListToServiceDto);
        given(feedService.serviceDtoToFeedDtoList(Mockito.any(FeedServiceDto.FeedListToServiceDto.class))).willReturn(feedDtoList);


        mockMvc.perform(
                        post("/feeds/list", memberId)
                                .params(params)
                                .header("Authorization", tokenProvider.createAccessToken(memberId))
                ).andExpect(status().isOk())
                .andExpect(content().json(content))
                .andExpect(jsonPath("$.responseList[0].feedId").value(1))
                .andExpect(jsonPath("$.responseList[1].feedId").value(2))
                .andExpect(jsonPath("$.responseList[0].memberInfo.memberId").value(1))
                .andExpect(jsonPath("$.responseList[1].memberInfo.memberId").value(1));
    }

    @Test
    @DisplayName("팔로우한 사용자 피드 리스트 가져오기 - 성공: 레디스 삭제 있음")
    void getFeedsByMemberFollow_Delete_Redis() throws Exception {
        long memberId = 1L;
        int page = 0, size = 10;
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", String.valueOf(page));
        params.add("size", String.valueOf(size));
        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = FeedServiceDto.FeedListToServiceDto.builder().build();

        FeedDtoList feedDtoList = FeedDtoList.builder()
                .responseList(Collections.emptyList())
                .build();
        String content = gson.toJson(feedDtoList);

        given(feedService.getFeedsByMemberFollow(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt())).willReturn(feedListToServiceDto);
        given(feedService.serviceDtoToFeedDtoList(Mockito.any(FeedServiceDto.FeedListToServiceDto.class))).willReturn(feedDtoList);
        given(feedService.getFeedsRecent(Mockito.anyLong(), Mockito.anyInt(), Mockito.anyInt())).willReturn(feedListToServiceDto);
        doNothing().when(feedService).deleteRedis(Mockito.anyLong());


        mockMvc.perform(
                        post("/feeds/list", memberId)
                                .params(params)
                                .header("Authorization", tokenProvider.createAccessToken(memberId))
                ).andExpect(status().isOk())
                .andExpect(content().json(content));
    }

    @Test
    @DisplayName("피드 생성 - 성공: 이미지가 있을 경우")
    void createFeed() throws Exception {
        long memberId = 1L;
        long feedId = 1L;

        List<MultipartFile> imageList = List.of(new MockMultipartFile("image", "gitimage.png", "image/png",
                new FileInputStream(getClass().getResource("/gitimage.png").getFile())));

        FeedServiceDto.Post post = FeedServiceDto.Post.builder()
                .content("content")
                .images(imageList)
                .build();

        FeedDto.Response response = getOneFeed(1L);

        String content = gson.toJson(response);
        FeedDto.Response feedDtoResponse = getOneFeed(feedId);

        given(feedService.createFeed(any(), Mockito.anyLong())).willReturn(feedId);
        given(feedService.changeFeedToFeedDtoResponse(feedId, memberId)).willReturn(feedDtoResponse);

        mockMvc.perform(
                multipart("/feeds")
                        .file("images",imageList.get(0).getBytes())
                        .header("Authorization", tokenProvider.createAccessToken(memberId))
                        .param("memberId", String.valueOf(memberId))
                        .param("content", "content")
        ).andExpect(status().isCreated())
                .andExpect(content().json(content));
    }

    @Test
    @DisplayName("피드 생성 - 성공: 이미지가 없을 경우")
    void createFeed_No_Image() throws Exception {
        long memberId = 1L;
        long feedId = 1L;

        List<MultipartFile> imageList = new ArrayList<>();

        FeedServiceDto.Post post = FeedServiceDto.Post.builder()
                .content("content")
                .images(imageList)
                .build();

        FeedDto.Response response = getOneFeed(1L);

        String content = gson.toJson(response);
        FeedDto.Response feedDtoResponse = getOneFeed(feedId);

        given(feedService.createFeed(any(), Mockito.anyLong())).willReturn(feedId);
        given(feedService.changeFeedToFeedDtoResponse(feedId, memberId)).willReturn(feedDtoResponse);

        mockMvc.perform(
                        multipart("/feeds")
                                .header("Authorization", tokenProvider.createAccessToken(memberId))
                                .param("memberId", String.valueOf(memberId))
                                .param("content", "content")
                ).andExpect(status().isCreated())
                .andExpect(content().json(content));
    }

    @Test
    @DisplayName("피드 수정 - 성공: 추가 이미지, 삭제 이미지 있을 경우")
    void patchFeed() throws Exception {
        long memberId = 1L;
        long feedId = 1L;
        List<MultipartFile> imageList = List.of(new MockMultipartFile("image", "gitimage.png", "image/png",
                new FileInputStream(getClass().getResource("/gitimage.png").getFile())));
        String[] deleteImages = new String[]{"image1.jpg", "image2.jpg"};

        FeedDto.Response response = getOneFeed(1L);
        String content = gson.toJson(response);

        given(feedService.patchFeed(any(FeedServiceDto.Patch.class), Mockito.anyLong())).willReturn(feedId);
        given(feedService.changeFeedToFeedDtoResponse(feedId, memberId)).willReturn(response);

        mockMvc.perform(
                    patch("/feeds/{feed-id}", feedId)
                            .content(imageList.get(0).getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .header("Authorization", tokenProvider.createAccessToken(memberId))
                            .param("deleteImages", deleteImages)
                            .param("content", "content")
            ).andExpect(status().isOk())
            .andExpect(content().json(content));

    }

    @Test
    @DisplayName("피드 수정 - 성공: 추가 이미지, 삭제 이미지 있을 경우")
    void patchFeed_No_Add_Delete_Image() throws Exception {
        long memberId = 1L;
        long feedId = 1L;

        FeedDto.Response response = getOneFeed(1L);
        String content = gson.toJson(response);

        given(feedService.patchFeed(any(FeedServiceDto.Patch.class), Mockito.anyLong())).willReturn(feedId);
        given(feedService.changeFeedToFeedDtoResponse(feedId, memberId)).willReturn(response);

        mockMvc.perform(
                        patch("/feeds/{feed-id}", feedId)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                                .header("Authorization", tokenProvider.createAccessToken(memberId))
                                .param("content", "content")
                ).andExpect(status().isOk())
                .andExpect(content().json(content));

    }

    @Test
    @DisplayName("피드 삭제")
    void deleteFeed() throws Exception {
        long feedId = 1L;
        long memberId = 1L;

        doNothing().when(feedService).deleteFeed(feedId, memberId);

        mockMvc.perform(
                delete("/feeds/{feed-id}", feedId)
                        .header("Authorization", tokenProvider.createAccessToken(memberId))
        ).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("피드 좋아요 테스트")
    void likeFeed() throws Exception {
        long memberId = 1L;
        long feedId = 1L;
        FeedDto.Like feedLikeDto = getLikeFeed();
        String content = gson.toJson(feedLikeDto);

        System.out.println(content);

        given(feedService.likeByMember(Mockito.anyLong(), Mockito.anyLong())).willReturn(feedLikeDto);

        mockMvc.perform(
                patch("/feeds/like/{feed-id}", feedId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", tokenProvider.createAccessToken(memberId))

        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(100))
                .andExpect(jsonPath("$.isLike").value(true));
    }

    private FeedDto.Like getLikeFeed() {
        return FeedDto.Like.builder()
                .likeCount(100)
                .isLike(true)
                .build();
    }

    private MemberDto.Info getMemberInfo(long memberId) {
        return MemberDto.Info.builder()
                .memberId(memberId)
                .nickname("사용자 " + memberId)
                .imageURL("http://image.jpg")
                .build();
    }

    private ImageDto getImageDto(long imageId) {
        return ImageDto.builder()
                .imageId(imageId)
                .originalFilename("originalFilename " + imageId)
                .uploadFileURL("http://FeedImage" + imageId + ".jpg")
                .build();
    }

    private FeedCommentDto.Response getFeedCommentDto(long memberId) {
        MemberDto.Info feedCommentMemberInfo = getMemberInfo(memberId);

        return FeedCommentDto.Response.builder()
                .feedCommentsId(memberId)
                .memberInfo(feedCommentMemberInfo)
                .content("feed content " + memberId)
                .createdAt(null)
                .modifiedAt(null)
                .build();
    }

    private FeedDto.Response getOneFeed(long feedId) {
        long memberId = 1L;

        MemberDto.Info feedMemberInfo = getMemberInfo(memberId);
        List<ImageDto> imageDtoList = new ArrayList<>();
        List<FeedCommentDto.Response> feedCommentDtoList = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            ImageDto imageDto = getImageDto(i);
            imageDtoList.add(imageDto);
            FeedCommentDto.Response feedCommentDto = getFeedCommentDto(i);
            feedCommentDtoList.add(feedCommentDto);
        }

        return FeedDto.Response.builder()
                .feedId(feedId)
                .memberInfo(feedMemberInfo)
                .images(imageDtoList)
                .content("feed content")
                .likes(1)
                .isLike(false)
                .feedComments(feedCommentDtoList)
                .shareURL("http://localhost:8080/feeds/all/" + feedId + "/0")
                .createdAt(null)
                .modifiedAt(null)
                .build();
    }

    private FeedDtoList getFeedList(int count) {
        List<FeedDto.Response> responseList = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            FeedDto.Response response = getOneFeed(i);
            responseList.add(response);
        }
        return FeedDtoList.builder()
                .responseList(responseList)
                .build();
    }

}