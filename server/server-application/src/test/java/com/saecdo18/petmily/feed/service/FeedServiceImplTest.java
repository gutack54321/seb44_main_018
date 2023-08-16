package com.saecdo18.petmily.feed.service;

import com.saecdo18.petmily.awsConfig.S3UploadService;
import com.saecdo18.petmily.feed.dto.FeedCommentDto;
import com.saecdo18.petmily.feed.dto.FeedDto;
import com.saecdo18.petmily.feed.dto.FeedDtoList;
import com.saecdo18.petmily.feed.dto.FeedServiceDto;
import com.saecdo18.petmily.feed.entity.Feed;
import com.saecdo18.petmily.feed.entity.FeedComments;
import com.saecdo18.petmily.feed.entity.FeedImage;
import com.saecdo18.petmily.feed.entity.FeedLike;
import com.saecdo18.petmily.feed.mapper.FeedMapper;
import com.saecdo18.petmily.feed.repository.FeedCommentsRepository;
import com.saecdo18.petmily.feed.repository.FeedImageRepository;
import com.saecdo18.petmily.feed.repository.FeedLikeRepository;
import com.saecdo18.petmily.feed.repository.FeedRepository;
import com.saecdo18.petmily.image.dto.ImageDto;
import com.saecdo18.petmily.image.entity.Image;
import com.saecdo18.petmily.image.repository.ImageRepository;
import com.saecdo18.petmily.member.dto.MemberDto;
import com.saecdo18.petmily.member.entity.FollowMember;
import com.saecdo18.petmily.member.entity.Member;
import com.saecdo18.petmily.member.repository.FollowMemberRepository;
import com.saecdo18.petmily.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    @Mock
    private FeedRepository feedRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private FollowMemberRepository followMemberRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private FeedImageRepository feedImageRepository;
    @Mock
    private S3UploadService s3UploadService;
    @Mock
    private FeedCommentsRepository feedCommentsRepository;
    @Mock
    private FeedLikeRepository feedLikeRepository;
    @Mock
    private FeedMapper feedMapper;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private FeedServiceImpl feedService;

    // 피드 생성 기능 =================================================
    @Test
    @DisplayName("피드 생성 성공 - 이미지 없음")
    void createFeedSuccessWhenNoImage() throws IOException {
        FeedServiceDto.Post post = FeedServiceDto.Post.builder()
                .content("content")
                .images(Collections.emptyList())
                .build();
        long memberId = 1L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Feed feed = Feed.builder()
                .content(post.getContent())
                .member(member)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", 1L);

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedRepository.save(Mockito.any(Feed.class))).willReturn(feed);

        Long feedId = feedService.createFeed(post, memberId);

        assertEquals(feedId, 1L);
    }

    @Test
    @DisplayName("피드 생성 성공 - 이미지 있음")
    void createFeedSuccessWhenHaveImage() throws IOException {
        long memberId = 1L;
        long feedId = 1L;
        String uploadFileURL = "http://image.jpg";
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "feedCount", 0);

        List<MultipartFile> imageList = List.of(new MockMultipartFile("image", "gitimage.png", "image/png",
                new FileInputStream(getClass().getResource("/gitimage.png").getFile())));
        FeedServiceDto.Post post = FeedServiceDto.Post.builder()
                .content("content")
                .images(imageList)
                .build();

        Feed feed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", feedId);

        Image image = Image.builder().uploadFileURL(uploadFileURL).build();
        FeedImage feedImage = FeedImage.builder().feed(feed).image(image).build();

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(s3UploadService.saveFile(Mockito.any(MultipartFile.class), Mockito.anyString())).willReturn(uploadFileURL);
        given(imageRepository.save(Mockito.any(Image.class))).willReturn(image);
        given(feedImageRepository.save(Mockito.any(FeedImage.class))).willReturn(feedImage);
        given(feedRepository.save(Mockito.any(Feed.class))).willReturn(feed);

        Long savedFeedId = feedService.createFeed(post, memberId);

        assertEquals(savedFeedId, feedId);
    }

    @Test
    @DisplayName("피드 생성 실패 - 사용자를 찾을 수 없습니다.")
    void createFeedFailsWhenMemberNotFound() throws IOException {
        long memberId = 1L;
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "feedCount", 0);

        List<MultipartFile> imageList = List.of(new MockMultipartFile("image", "gitimage.png", "image/png",
                new FileInputStream(getClass().getResource("/gitimage.png").getFile())));
        FeedServiceDto.Post post = FeedServiceDto.Post.builder()
                .content("content")
                .images(imageList)
                .build();

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> feedService.createFeed(post, memberId));

        assertEquals(exception.getMessage(), "사용자를 찾을 수 없습니다.");
    }

    // 단일 피드 가져오기 (Guest) 기능 =================================================

    @Test
    @DisplayName("게스트용 피드 가져오기 - 성공")
    void getFeedsRecent_For_Guest() {
        int page = 0, size = 10;
        Feed feed = Feed.builder().build();
        ReflectionTestUtils.setField(feed, "feedId", 1L);
        Page<Feed> feedPage = new PageImpl<>(List.of(feed));
        given(feedRepository.findAll(Mockito.any(PageRequest.class))).willReturn(feedPage);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsRecentForGuest(page, size);

        assertEquals(feedListToServiceDto.getFeedList().size(), 1);
        assertEquals(feedListToServiceDto.getFeedList().get(0).getFeedId(), 1L);
    }

    // 단일 피드 가져오기 기능 =========================================================

    @Test
    @DisplayName("피드 가져오기 성공 - feedComment empty & no image & memberId != 0")
    void changeFeedToFeedDtoResponseSuccess(){
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", 1L);

        Feed findFeed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedId", 1L);

        FeedDto.Response response = FeedDto.Response.builder()
                .feedId(findFeed.getFeedId())
                .content(findFeed.getContent())
                .build();

        MemberDto.Info memberInfo = MemberDto.Info.builder()
                .memberId(member.getMemberId())
                .build();
        response.setMemberInfo(memberInfo);

        response.setImages(new ArrayList<>());

        FeedLike feedLike = FeedLike.builder()
                .feed(findFeed)
                .member(member)
                .build();
        ReflectionTestUtils.setField(feedLike, "isLike", true);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(findFeed));
        given(feedMapper.FeedToFeedDtoResponse(Mockito.any(Feed.class))).willReturn(response);
        given(feedCommentsRepository.findByFeedFeedId(Mockito.anyLong())).willReturn(Optional.of(Collections.emptyList()));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedLikeRepository.findByMemberAndFeed(Mockito.any(Member.class), Mockito.any(Feed.class))).willReturn(Optional.of(feedLike));

        FeedDto.Response result = feedService.changeFeedToFeedDtoResponse(1L, 1L);

        assertEquals(result.getFeedId(), 1L);
        assertEquals(result.getMemberInfo().getMemberId(), 1L);
        assertTrue(result.isLike());

    }

    private MemberDto.Info makeMemberInfo(long memberId) {
        return MemberDto.Info.builder()
                .memberId(memberId)
                .build();
    }

    @Test
    @DisplayName("피드 가져오기 성공 - no image & memberId != 0")
    void changeFeedToFeedDtoResponseSuccess_Add_feedComment(){
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", 1L);

        Feed findFeed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedId", 1L);

        FeedComments feedComments = FeedComments.builder()
                .feed(findFeed)
                .member(member)
                .content("댓글")
                .build();
        ReflectionTestUtils.setField(feedComments, "feedCommentsId", 1L);

        List<FeedComments> feedCommentsList = List.of(feedComments);

        FeedCommentDto.Response feedCommentResponse = FeedCommentDto.Response.builder()
                .feedCommentsId(feedComments.getFeedCommentsId())
                .content(feedComments.getContent())
                .memberInfo(makeMemberInfo(2L))
                .build();
        List<FeedCommentDto.Response> feedCommentDtoList = List.of(feedCommentResponse);


        FeedDto.Response response = FeedDto.Response.builder()
                .feedId(findFeed.getFeedId())
                .content(findFeed.getContent())
                .feedComments(feedCommentDtoList)
                .build();

        response.setMemberInfo(makeMemberInfo(member.getMemberId()));

        response.setImages(new ArrayList<>());

        FeedLike feedLike = FeedLike.builder()
                .feed(findFeed)
                .member(member)
                .build();
        ReflectionTestUtils.setField(feedLike, "isLike", true);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(findFeed));
        given(feedMapper.FeedToFeedDtoResponse(Mockito.any(Feed.class))).willReturn(response);
        given(feedCommentsRepository.findByFeedFeedId(Mockito.anyLong())).willReturn(Optional.of(feedCommentsList));
        given(feedMapper.feedCommentsToFeedCommentDto(Mockito.any(FeedComments.class))).willReturn(feedCommentResponse);
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedLikeRepository.findByMemberAndFeed(Mockito.any(Member.class), Mockito.any(Feed.class))).willReturn(Optional.of(feedLike));

        FeedDto.Response result = feedService.changeFeedToFeedDtoResponse(1L, 1L);

        assertEquals(result.getFeedId(), 1L);
        assertEquals(result.getMemberInfo().getMemberId(), 1L);
        assertTrue(result.isLike());
        assertEquals(result.getFeedComments().size(), 1);
        assertEquals(result.getFeedComments().get(0).getFeedCommentsId(), 1L);

    }

    @Test
    @DisplayName("피드 가져오기 성공 - memberId != 0")
    void changeFeedToFeedDtoResponseSuccess_Add_feedComment_And_Image(){
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", 1L);

        Feed findFeed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedId", 1L);

        FeedImage feedImage = FeedImage.builder()
                .image(new Image())
                .feed(findFeed)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedImageList", List.of(feedImage));

        FeedComments feedComments = FeedComments.builder()
                .feed(findFeed)
                .member(member)
                .content("댓글")
                .build();
        ReflectionTestUtils.setField(feedComments, "feedCommentsId", 1L);

        List<FeedComments> feedCommentsList = List.of(feedComments);

        FeedCommentDto.Response feedCommentResponse = FeedCommentDto.Response.builder()
                .feedCommentsId(feedComments.getFeedCommentsId())
                .content(feedComments.getContent())
                .memberInfo(makeMemberInfo(2L))
                .build();
        List<FeedCommentDto.Response> feedCommentDtoList = List.of(feedCommentResponse);


        FeedDto.Response response = FeedDto.Response.builder()
                .feedId(findFeed.getFeedId())
                .content(findFeed.getContent())
                .feedComments(feedCommentDtoList)
                .build();

        response.setMemberInfo(makeMemberInfo(member.getMemberId()));

        ImageDto imageDto = ImageDto.builder()
                .imageId(1L)
                .uploadFileURL("http://image.jpg")
                .originalFilename("image.jpg")
                .build();
        List<ImageDto> imageDtoList = List.of(imageDto);
        response.setImages(imageDtoList);

        FeedLike feedLike = FeedLike.builder()
                .feed(findFeed)
                .member(member)
                .build();
        ReflectionTestUtils.setField(feedLike, "isLike", true);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(findFeed));
        given(feedMapper.FeedToFeedDtoResponse(Mockito.any(Feed.class))).willReturn(response);
        given(feedCommentsRepository.findByFeedFeedId(Mockito.anyLong())).willReturn(Optional.of(feedCommentsList));
        given(feedMapper.feedCommentsToFeedCommentDto(Mockito.any(FeedComments.class))).willReturn(feedCommentResponse);
        given(feedMapper.imageToImageDto(Mockito.any(Image.class))).willReturn(imageDto);
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedLikeRepository.findByMemberAndFeed(Mockito.any(Member.class), Mockito.any(Feed.class))).willReturn(Optional.of(feedLike));

        FeedDto.Response result = feedService.changeFeedToFeedDtoResponse(1L, 1L);

        assertEquals(result.getFeedId(), 1L);
        assertEquals(result.getMemberInfo().getMemberId(), 1L);
        assertTrue(result.isLike());
        assertEquals(result.getFeedComments().size(), 1);
        assertEquals(result.getFeedComments().get(0).getFeedCommentsId(), 1L);
        assertEquals(result.getImages().size(), 1);
        assertEquals(result.getImages().get(0).getImageId(), 1L);
    }

    @Test
    @DisplayName("피드 가져오기 성공 - 모든 조건 만족")
    void changeFeedToFeedDtoResponseSuccess_Add_feedComment_And_Image_And_MemberId_0(){
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", 1L);

        Feed findFeed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedId", 1L);

        FeedImage feedImage = FeedImage.builder()
                .image(new Image())
                .feed(findFeed)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedImageList", List.of(feedImage));

        FeedComments feedComments = FeedComments.builder()
                .feed(findFeed)
                .member(member)
                .content("댓글")
                .build();
        ReflectionTestUtils.setField(feedComments, "feedCommentsId", 1L);

        List<FeedComments> feedCommentsList = List.of(feedComments);

        FeedCommentDto.Response feedCommentResponse = FeedCommentDto.Response.builder()
                .feedCommentsId(feedComments.getFeedCommentsId())
                .content(feedComments.getContent())
                .memberInfo(makeMemberInfo(2L))
                .build();
        List<FeedCommentDto.Response> feedCommentDtoList = List.of(feedCommentResponse);


        FeedDto.Response response = FeedDto.Response.builder()
                .feedId(findFeed.getFeedId())
                .content(findFeed.getContent())
                .feedComments(feedCommentDtoList)
                .build();

        response.setMemberInfo(makeMemberInfo(member.getMemberId()));

        ImageDto imageDto = ImageDto.builder()
                .imageId(1L)
                .uploadFileURL("http://image.jpg")
                .originalFilename("image.jpg")
                .build();
        List<ImageDto> imageDtoList = List.of(imageDto);
        response.setImages(imageDtoList);
        response.setLike(false);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(findFeed));
        given(feedMapper.FeedToFeedDtoResponse(Mockito.any(Feed.class))).willReturn(response);
        given(feedCommentsRepository.findByFeedFeedId(Mockito.anyLong())).willReturn(Optional.of(feedCommentsList));
        given(feedMapper.feedCommentsToFeedCommentDto(Mockito.any(FeedComments.class))).willReturn(feedCommentResponse);
        given(feedMapper.imageToImageDto(Mockito.any(Image.class))).willReturn(imageDto);
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));

        FeedDto.Response result = feedService.changeFeedToFeedDtoResponse(1L, 1L);

        assertEquals(result.getFeedId(), 1L);
        assertEquals(result.getMemberInfo().getMemberId(), 1L);
        assertFalse(result.isLike());
        assertEquals(result.getFeedComments().size(), 1);
        assertEquals(result.getFeedComments().get(0).getFeedCommentsId(), 1L);
        assertEquals(result.getImages().size(), 1);
        assertEquals(result.getImages().get(0).getImageId(), 1L);
    }

    @Test
    @DisplayName("피드 가져오기 실패 - 피드를 찾을 수 없습니다.")
    void changeFeedToFeedDtoResponseFailsWhenFeedNotFound() {
        long feedId = 1L;
        long memberId = 1L;
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        Feed findFeed = Feed.builder().member(member).build();
        ReflectionTestUtils.setField(findFeed, "feedId", feedId);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> feedService.changeFeedToFeedDtoResponse(feedId, memberId));

        assertEquals(exception.getMessage(), "피드를 찾을 수 없습니다.");
    }

    // 피드 최신 순 리스트 가져오기 기능 =================================================

    @Test
    @DisplayName("피드 최신 순 가져오기 성공")
    void getFeedsRecent() {
        long totalCount = 1L;
        long memberId = 1L;
        int page = 0, size = 10;

        List<Feed> feedList = feedList(size);
        Page<Feed> feedPage = new PageImpl<>(feedList);

        given(feedRepository.count()).willReturn(totalCount);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.rangeByScore(Mockito.anyString(), Mockito.anyDouble(), Mockito.anyDouble()))
                .willReturn(Collections.emptySet());
        given(feedRepository.findAll(Mockito.any(PageRequest.class))).willReturn(feedPage);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsRecent(memberId, page, size);

        assertEquals(feedListToServiceDto.getFeedList().size(), 10);

    }

    @Test
    @DisplayName("피드 최신 순 가져오기 - 성공: 중복 피드 제거")
    void getFeedsRecent_중복_피드_제거() {
        long totalCount = 10L;
        long memberId = 1L;
        int page = 0, size = 10;

        List<Feed> feedList = feedList(size);
        Page<Feed> feedPage = new PageImpl<>(feedList);
        Set<String> previousIds = new HashSet<>();
        previousIds.add("1");


        given(feedRepository.count()).willReturn(totalCount);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.rangeByScore(Mockito.anyString(), Mockito.anyDouble(), Mockito.anyDouble()))
                .willReturn(previousIds);
        given(feedRepository.findAll(Mockito.any(PageRequest.class))).willReturn(feedPage);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsRecent(memberId, page, size);

        assertEquals(feedListToServiceDto.getFeedList().size(), 9);
    }

    @Test
    @DisplayName("피드 최신 순 가져오기 - 성공: 가져올 피드가 없어 size = 0")
    void getFeedsRecent_FeedList_Size_0() {
        long totalCount = 10L;
        long memberId = 1L;
        int page = 2, size = 5;

        List<Feed> feedList = feedList(0);
        Page<Feed> feedPage = new PageImpl<>(feedList);


        given(feedRepository.count()).willReturn(totalCount);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.rangeByScore(Mockito.anyString(), Mockito.anyDouble(), Mockito.anyDouble()))
                .willReturn(Collections.emptySet());
        given(feedRepository.findAll(Mockito.any(PageRequest.class))).willReturn(feedPage);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsRecent(memberId, page, size);

        assertEquals(feedListToServiceDto.getFeedList().size(), 0);
    }

    @Test
    @DisplayName("피드 최신 순 가져오기 - 성공: size에 맞게 피드 자르기")
    void getFeedsRecent_FeedList_Cut() {
        long totalCount = 10L;
        long memberId = 1L;
        int page = 0, size = 5;

        List<Feed> feedList = feedList(6);
        Page<Feed> feedPage = new PageImpl<>(feedList);


        given(feedRepository.count()).willReturn(totalCount);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.rangeByScore(Mockito.anyString(), Mockito.anyDouble(), Mockito.anyDouble()))
                .willReturn(Collections.emptySet());
        given(feedRepository.findAll(Mockito.any(PageRequest.class))).willReturn(feedPage);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsRecent(memberId, page, size);

        assertEquals(feedListToServiceDto.getFeedList().size(), 5);
    }

    private List<Feed> feedList(int size) {
        List<Feed> feedList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Feed feed = Feed.builder()
                    .content("content" + "i")
                    .build();
            ReflectionTestUtils.setField(feed, "feedId", (long) i);
            feedList.add(feed);
        }

        return feedList;
    }

    // 사용자 피드 가져오기 기능 ==================================================
    @Test
    @DisplayName("사용자 피드 리스트 가져오기 성공")
    void getFeedsByMember() {
        int page = 0;
        int size = 10;
        long memberId = 1L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Feed feed1 = Feed.builder().build();
        ReflectionTestUtils.setField(feed1, "feedId", 1L);
        Feed feed2 = Feed.builder().build();
        ReflectionTestUtils.setField(feed2, "feedId", 1L);

        List<Feed> feedList = new ArrayList<>();
        feedList.add(feed1);
        feedList.add(feed2);

        Page<Feed> feedPage = new PageImpl<>(feedList);

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedRepository.findAllByMemberOrderByCreatedAtDesc(Mockito.any(Member.class), Mockito.any(PageRequest.class))).willReturn(feedPage);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsByMember(page, size, memberId);

        assertEquals(feedListToServiceDto.getFeedList().size(), 2);
    }

    @Test
    @DisplayName("사용자 피드 리스트 가져오기 - 성공: 피드 없음")
    void getFeedsByMember_Feed_0() {
        int page = 0;
        int size = 10;
        long memberId = 1L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        List<Feed> feedList = new ArrayList<>();

        Page<Feed> feedPage = new PageImpl<>(feedList);

        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedRepository.findAllByMemberOrderByCreatedAtDesc(Mockito.any(Member.class), Mockito.any(PageRequest.class))).willReturn(feedPage);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsByMember(page, size, memberId);

        assertEquals(feedListToServiceDto.getFeedList().size(), 0);
    }

    // 팔로우한 사용자들 리스트 가져오기 기능 ========================================

    @Test
    @DisplayName("팔로우한 사용자들의 리스트 가져오기 성공 - 팔로우한 멤버가 없음")
    void getFeedsByMemberFollow() {
        long memberId = 1L;
        int page = 0, size = 10;

        given(followMemberRepository.findByFollowingId(Mockito.anyLong())).willReturn(Optional.of(Collections.emptyList()));
        given(redisTemplate.hasKey(Mockito.anyString())).willReturn(true);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.rangeByScore(Mockito.anyString(), Mockito.anyDouble(), Mockito.anyDouble()))
                .willReturn(Collections.emptySet());

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsByMemberFollow(memberId, page, size);

        assertEquals(feedListToServiceDto.getFeedList().size(), 0);
    }

    @Test
    @DisplayName("팔로우한 사용자들의 리스트 가져오기 성공 - 팔로우한 멤버가 있음")
    void getFeedsByMemberFollow_팔로우_있음() {
        long memberId = 1L;
        int page = 0, size = 10;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", 2L);

        Feed feed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", 1L);

        FollowMember followMember = FollowMember.builder()
                .followingId(memberId)
                .followerMember(member)
                .build();

        List<FollowMember> followMemberList = List.of(followMember);

        given(followMemberRepository.findByFollowingId(Mockito.anyLong())).willReturn(Optional.of(followMemberList));
        given(redisTemplate.hasKey(Mockito.anyString())).willReturn(true);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.rangeByScore(Mockito.anyString(), Mockito.anyDouble(), Mockito.anyDouble()))
                .willReturn(Collections.emptySet());
        given(feedRepository.findFirstByMemberOrderByCreatedAtDesc(Mockito.any(Member.class))).willReturn(feed);

        FeedServiceDto.FeedListToServiceDto feedListToServiceDto = feedService.getFeedsByMemberFollow(memberId, page, size);

        assertEquals(feedListToServiceDto.getFeedList().size(), 1);
    }

    // 피드 수정 기능 ========================================================================
    @Test
    @DisplayName("피드 수정 성공 - 추가 & 삭제 이미지 없음")
    void patchFeedSuccess() throws IOException {
        long feedId = 1L;
        long memberId = 1L;

        FeedServiceDto.Patch patch = FeedServiceDto.Patch.builder()
                .feedId(feedId)
                .content("patch content")
                .build();
        ReflectionTestUtils.setField(patch, "addImages", Collections.emptyList());
        ReflectionTestUtils.setField(patch, "deleteImages", Collections.emptyList());


        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Feed findFeed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedId", feedId);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(findFeed));

        Long patchedFeedId = feedService.patchFeed(patch, memberId);

        assertEquals(patchedFeedId, feedId);
    }

    @Test
    @DisplayName("피드 수정 성공 - 이미지 추가")
    void patchFeedSuccess_Add_Image() throws IOException {
        long feedId = 1L;
        long memberId = 1L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Feed findFeed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedId", feedId);

        List<String> deleteImages = List.of("image.jpg");

        List<FeedImage> feedImageList = List.of(
                FeedImage.builder()
                        .image(Image.builder()
                                .originalFilename("image.jpg")
                                .build())
                        .feed(findFeed)
                        .build()
        );

        FeedServiceDto.Patch patch = FeedServiceDto.Patch.builder()
                .feedId(feedId)
                .content("patch content")
                .addImages(Collections.emptyList())
                .deleteImages(deleteImages)
                .build();

        ReflectionTestUtils.setField(findFeed, "feedImageList", feedImageList);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(findFeed));
        doNothing().when(s3UploadService).deleteImage(Mockito.anyString());
        doNothing().when(feedImageRepository).delete(Mockito.any(FeedImage.class));

        Long patchedFeedId = feedService.patchFeed(patch, memberId);

        assertEquals(patchedFeedId, feedId);
    }

    @Test
    @DisplayName("피드 수정 성공 - 이미지 삭제")
    void patchFeedSuccess_Delete_Image() throws IOException {
        long feedId = 1L;
        long memberId = 1L;
        String uploadFileURL = "http://image.jpg";

        List<MultipartFile> imageList = List.of(new MockMultipartFile("image", "gitimage.png", "image/png",
                new FileInputStream(getClass().getResource("/gitimage.png").getFile())));
        Image image = Image.builder()
                .uploadFileURL(uploadFileURL)
                .originalFilename("original")
                .build();

        FeedServiceDto.Patch patch = FeedServiceDto.Patch.builder()
                .feedId(feedId)
                .content("patch content")
                .addImages(imageList)
                .build();
        ReflectionTestUtils.setField(patch, "deleteImages", Collections.emptyList());

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Feed findFeed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(findFeed, "feedId", feedId);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(findFeed));
        given(s3UploadService.saveFile(Mockito.any(MultipartFile.class), Mockito.anyString())).willReturn(uploadFileURL);
        given(imageRepository.save(Mockito.any(Image.class))).willReturn(image);

        Long patchedFeedId = feedService.patchFeed(patch, memberId);

        assertEquals(patchedFeedId, feedId);
    }

    @Test
    @DisplayName("피드 수정 실패 - 수정할 권한이 없습니다.")
    void patchFeedFails() {
        long memberId = 1L;
        Member member = new Member();
        FeedServiceDto.Patch patch = FeedServiceDto.Patch.builder().feedId(1L).build();
        ReflectionTestUtils.setField(member, "memberId", 2L);
        Feed feed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(feed));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> feedService.patchFeed(patch, memberId));

        assertEquals(exception.getMessage(), "수정할 권한이 없습니다.");
    }

    // 피드 삭제 기능 =========================================================================

    @Test
    @DisplayName("피드 삭제 성공")
    void deleteFeedSuccess() {
        // given
        long feedId = 1L;
        long memberId = 1L;

        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);

        Feed feed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", feedId);
        Image image = Image.builder().originalFilename("image.jpg").uploadFileURL("http://image.jpg").build();
        FeedImage feedImage = FeedImage.builder().feed(feed).image(image).build();
        List<FeedImage> feedImageList = List.of(feedImage);
        ReflectionTestUtils.setField(feed, "feedImageList", feedImageList);

        given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        doNothing().when(s3UploadService).deleteImage(Mockito.anyString());
        doNothing().when(feedRepository).delete(feed);

        // when
        feedService.deleteFeed(feedId, memberId);

        verify(feedRepository, times(1)).delete(feed);
        verify(s3UploadService, times(feed.getFeedImageList().size())).deleteImage(Mockito.anyString());
    }

    @Test
    @DisplayName("피드 삭제 실패 - 피드를 찾을 수 없습니다.")
    void deleteFeedFailsWhenFeedNotFound() {
        // given
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", 1L);
        Feed feed =  new Feed();
        ReflectionTestUtils.setField(feed, "feedId", 1L);
        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());

        // when, then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> feedService.deleteFeed(feed.getFeedId(), member.getMemberId()));
        assertEquals("피드를 찾을 수 없습니다.", exception.getMessage());
        verify(feedRepository, never()).delete(any(Feed.class));
        verify(s3UploadService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("피드 삭제 실패 - 삭제를 요청한 사용자와 피드를 작성한 사용자가 다르다.")
    void deleteFeedFailsWhenFeedMemberNotEqualsRequestMember() {
        // given
        long feedId = 1L;
        long feedMemberId = 2L;
        long requestMemberId = 3L;
        Member feedMember = new Member();
        ReflectionTestUtils.setField(feedMember, "memberId", feedMemberId);
        Feed feed = Feed.builder()
                .content("content")
                .member(feedMember)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", feedId);
        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(feed));

        // when, then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                feedService.deleteFeed(feedId, requestMemberId));

        assertEquals("삭제할 권한이 없습니다.", exception.getMessage());
        verify(feedRepository, never()).delete(any(Feed.class));
        verify(s3UploadService, never()).deleteImage(anyString());

    }

    @Test
    @DisplayName("피드 삭제 실패 - 사용자를 찾을 수 없습니다.")
    void deleteFeedFailsWhenMemberNotFound() {
        // given
        long feedId = 1L;
        long memberId = 1L;
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        Feed feed =  new Feed();
        ReflectionTestUtils.setField(feed, "feedId", feedId);
        ReflectionTestUtils.setField(feed, "member", member);
        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(feed));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());

        // when
        RuntimeException exception = assertThrows(RuntimeException.class, () -> feedService.deleteFeed(feedId, memberId));

        // then
        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
        verify(feedRepository, never()).delete(any(Feed.class));
        verify(s3UploadService, never()).deleteImage(anyString());
    }

    // 이미지 저장 기능 =========================================================
    @Test
    @DisplayName("이미지 저장")
    void saveImage() {
        // given
        Feed feed = new Feed();
        String originalFilename = "image.jpg";
        String uploadFileURL = "http://example.com/image.jpg";
        Image image = Image.builder()
                .originalFilename(originalFilename)
                .uploadFileURL(uploadFileURL)
                .build();

        FeedImage feedImage = FeedImage.builder()
                .feed(feed)
                .image(image)
                .build();

        // when
        when(imageRepository.save(any(Image.class))).thenReturn(image);
        when(feedImageRepository.save(any(FeedImage.class))).thenReturn(feedImage);

        // then
        assertSame(image, imageRepository.save(image));
        assertSame(feedImage, feedImageRepository.save(feedImage));

    }
    // 피드 좋아요 기능 ======================================================
    @Test
    @DisplayName("피드 좋아요 성공 - 좋아요 등록")
    void likeByMemberSuccess_Like() {
        long memberId = 1L;
        long feedId = 1L;
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        Feed feed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", feedId);
        FeedLike feedLike = FeedLike.builder()
                .feed(feed)
                .member(member)
                .build();
        ReflectionTestUtils.setField(feedLike, "feedLikeId", 1L);
        ReflectionTestUtils.setField(feedLike, "isLike", true);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(feed));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedLikeRepository.findByMemberAndFeed(Mockito.any(Member.class), Mockito.any(Feed.class))).willReturn(Optional.empty());
        given(feedLikeRepository.save(Mockito.any(FeedLike.class))).willReturn(feedLike);
        given(feedRepository.save(Mockito.any(Feed.class))).willReturn(feed);

        // when
        FeedDto.Like feedDtoLike = feedService.likeByMember(feedId, memberId);

        // then
        assertEquals(feedDtoLike.getLikeCount(), 1);
        assertTrue(feedDtoLike.isLike());
    }

    @Test
    @DisplayName("피드 좋아요 성공 - 좋아요 해제")
    void likeByMemberSuccess_UnLike() {
        long memberId = 1L;
        long feedId = 1L;
        Member member = new Member();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        Feed feed = Feed.builder()
                .content("content")
                .member(member)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", feedId);
        ReflectionTestUtils.setField(feed, "likes", 1);
        FeedLike feedLike = FeedLike.builder()
                .feed(feed)
                .member(member)
                .build();
        ReflectionTestUtils.setField(feedLike, "feedLikeId", 1L);
        ReflectionTestUtils.setField(feedLike, "isLike", true);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(feed));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.of(member));
        given(feedLikeRepository.findByMemberAndFeed(Mockito.any(Member.class), Mockito.any(Feed.class))).willReturn(Optional.of(feedLike));
        given(feedLikeRepository.save(Mockito.any(FeedLike.class))).willReturn(feedLike);
        given(feedRepository.save(Mockito.any(Feed.class))).willReturn(feed);

        // when
        FeedDto.Like feedDtoLike = feedService.likeByMember(feedId, memberId);

        // then
        assertEquals(feedDtoLike.getLikeCount(), 0);
        assertFalse(feedDtoLike.isLike());
    }

    @Test
    @DisplayName("피드 좋아요 실패 - 피드를 찾을 수 없음.")
    void likeByMemberFailsWhenFeedNotFound() {
        long memberId = 1L;
        long feedId = 1L;
        Feed feed = new Feed();
        Member member = new Member();
        FeedLike feedLike = FeedLike.builder()
                .feed(feed)
                .member(member)
                .build();
        ReflectionTestUtils.setField(feed, "feedId", feedId);
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(feedLike, "feedLikeId", 1L);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> feedService.likeByMember(feedId, memberId));

        assertEquals("피드를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("피드 좋아요 실패 - 사용자를 찾을 수 없음.")
    void likeByMemberFailsWhenMemberNotFound() {
        long memberId = 1L;
        long feedId = 1L;
        Feed feed = new Feed();
        ReflectionTestUtils.setField(feed, "feedId", feedId);

        given(feedRepository.findById(Mockito.anyLong())).willReturn(Optional.of(feed));
        given(memberRepository.findById(Mockito.anyLong())).willReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> feedService.likeByMember(feedId, memberId));

        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());
    }


}