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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedRepository feedRepository;
    private final MemberRepository memberRepository;
    private final FollowMemberRepository followMemberRepository;
    private final ImageRepository imageRepository;
    private final FeedImageRepository feedImageRepository;
    private final S3UploadService s3UploadService;
    private final FeedCommentsRepository feedCommentsRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedMapper feedMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final static String BASE_URI = "http://15.165.146.215:8080/feeds/all/";

    @Override
    public Long createFeed(FeedServiceDto.Post post, long memberId) throws IOException {
        Member findMember = methodFindByMemberId(memberId);
        Feed createFeed = Feed.builder()
                .content(post.getContent())
                .member(findMember)
                .build();

        if (!post.getImages().isEmpty()) {
            for (MultipartFile multipartFile : post.getImages()) {

                String originalFilename = multipartFile.getOriginalFilename()+ UUID.randomUUID();
                String uploadFileURL = s3UploadService.saveFile(multipartFile, originalFilename);
                saveImage(createFeed, originalFilename, uploadFileURL);
            }
        }
        Feed saveFeed = feedRepository.save(createFeed);
        findMember.upCountFeed();

        return saveFeed.getFeedId();
    }

//    @Override
//    public FeedDto.Response getFeed(long feedId, long memberId) {
//        Feed findFeed = methodFindByFeedId(feedId);
//        return changeFeedToFeedDtoResponse(findFeed, memberId);
//    }

    public FeedServiceDto.FeedListToServiceDto getFeedsRecentForGuest(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Feed> feedList = feedRepository.findAll(pageRequest).getContent();
        return FeedServiceDto.FeedListToServiceDto.builder()
                .feedList(feedList)
                .build();
    }

    public FeedDto.Response changeFeedToFeedDtoResponse(Long feedId, long memberId) {
        Feed feed = methodFindByFeedId(feedId);
        FeedDto.Response response = feedMapper.FeedToFeedDtoResponse(feed);
        List<FeedCommentDto.Response> feedCommentDtoList = methodFindFeedCommentByFeedId(feed.getFeedId());
        if (!feedCommentDtoList.isEmpty()) {
            response.setFeedComments(feedCommentDtoList);
        }
        response.setMemberInfo(memberIdToMemberInfoDto(feed.getMember().getMemberId()));

        List<FeedImage> feedImageList = feedImageRepository.findByFeed(feed);
        if(feedImageList != null)
            response.setImages(feedImageToImageDtoList(feedImageList));

        if (memberId == 0) {
            response.setLike(false);
        } else {
            response.setLike(feedLikesByMember(feed, methodFindByMemberId(memberId)));
        }
        response.setShareURL(feed.getShareURI(BASE_URI).toString());

        return response;
    }

    public FeedDtoList changeFeedListToFeedResponseDto(FeedServiceDto.FeedListToServiceDto feedListToServiceDto,
                                                       long memberId) {

        List<FeedDto.Response> responseList = new ArrayList<>();
        for (Feed feed : feedListToServiceDto.getFeedList()) {
            FeedDto.Response response = changeFeedToFeedDtoResponse(feed.getFeedId(), memberId);
            responseList.add(response);
        }

        return FeedDtoList.builder()
                .responseList(responseList)
                .build();
    }

    @Override
    public FeedServiceDto.FeedListToServiceDto getFeedsRecent(long memberId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Feed> feedList = new ArrayList<>();
        long totalCount = feedRepository.count();
        Set<String> previousIds = getToRedis(memberId);


        while (feedList.size() < size) {
            Page<Feed> feedPage = feedRepository.findAll(pageRequest);
            List<Feed> pageDataList = feedPage.getContent();

            List<Feed> filteredDataList = pageDataList.stream()
                    .filter(data -> !previousIds.contains(data.getFeedId().toString()))
                    .collect(Collectors.toList());

            feedList.addAll(filteredDataList);
            page++;
            if ((long) page * size >= totalCount) {
                int sum = page*size;
                break;
            }
            pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (feedList.size() > size) {
            feedList = feedList.subList(0, size);
        }
        addToRedisSet(feedList, memberId);

        Collections.shuffle(feedList);
        return FeedServiceDto.FeedListToServiceDto.builder()
                .feedList(feedList)
                .build();
    }

    @Override
    public FeedServiceDto.FeedListToServiceDto getFeedsByMember(int page, int size, long memberId) {
        Member findMember = methodFindByMemberId(memberId);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Feed> feedPage = feedRepository.findAllByMemberOrderByCreatedAtDesc(findMember, pageRequest);
        List<Feed> feedList = feedPage.getContent();

        return FeedServiceDto.FeedListToServiceDto.builder()
                .feedList(feedList)
                .build();
    }

    @Override
    public FeedServiceDto.FeedListToServiceDto getFeedsByMemberFollow(long memberId, int page, int size) {
        List<FollowMember> followMemberList = followMemberRepository.findByFollowingId(memberId).orElse(Collections.emptyList());

        Set<String> previousIds = getToRedis(memberId);

        List<Feed> feedList = new ArrayList<>();
        for(FollowMember followMember : followMemberList){
            Feed followFeed = feedRepository.findFirstByMemberOrderByCreatedAtDesc(followMember.getFollowerMember());
            if(!previousIds.contains(followFeed.getFeedId().toString()))
                feedList.add(followFeed);
        }
        if (feedList.size() > size) {
            feedList = feedList.subList(0, size);
        }


        Collections.shuffle(feedList);
        addToRedisSet(feedList, memberId);

        return FeedServiceDto.FeedListToServiceDto.builder()
                .feedList(feedList)
                .build();
    }

//    private List<Feed> filterFeedsByPreviousListIds(List<Feed> feedList, Set<String> previousIds) {
//        List<Feed> filterFeeds = new ArrayList<>();
//        for (Feed feed : feedList) {
//            if (!previousIds.contains(feed.getFeedId().toString())) {
//                filterFeeds.add(feed);
//            }
//        }
//        return filterFeeds;
//    }

    @Override
    public Long patchFeed(FeedServiceDto.Patch patch, long memberId) throws IOException {
        Feed findFeed = methodFindByFeedId(patch.getFeedId());
        if(memberId !=findFeed.getMember().getMemberId())
            throw new IllegalArgumentException("수정할 권한이 없습니다.");
        findFeed.updateContent(patch.getContent());

        if (!patch.getAddImages().isEmpty()) {
            for (MultipartFile multipartFile : patch.getAddImages()) {
                String originalFilename = multipartFile.getOriginalFilename()+ UUID.randomUUID();
                String uploadFileURL = s3UploadService.saveFile(multipartFile,originalFilename);
                saveImage(findFeed, originalFilename, uploadFileURL);
            }
        }
        if (!patch.getDeleteImages().isEmpty()) {
            for (String originalFilename : patch.getDeleteImages()) {
                for (FeedImage feedImage : findFeed.getFeedImageList()) {
                    if (originalFilename.equals(feedImage.getImage().getOriginalFilename())) {
                        s3UploadService.deleteImage(feedImage.getImage().getOriginalFilename());
                        feedImageRepository.delete(feedImage);
                    }
                }
            }
        }

        return findFeed.getFeedId();
    }

    @Override
    public void deleteFeed(long feedId, long memberId) {
        Feed findFeed = methodFindByFeedId(feedId);
        if (findFeed.getMember().getMemberId() == memberId) {
            Member findMember = methodFindByMemberId(memberId);
            findMember.downCountFeed();      // 피드 삭제시 멤버의 피드카운트 삭감

            for (FeedImage feedImage : findFeed.getFeedImageList()) {
                s3UploadService.deleteImage(feedImage.getImage().getOriginalFilename());
            }
            feedRepository.delete(findFeed);
            feedRepository.flush();
        }else
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
    }


    @Override
    public void saveImage(Feed feed, String originalFilename, String uploadFileURL) {
        Image image = Image.builder()
                .originalFilename(originalFilename)
                .uploadFileURL(uploadFileURL)
                .build();
        Image saveImage = imageRepository.save(image);
        FeedImage feedImage = FeedImage.builder()
                .feed(feed)
                .image(saveImage)
                .build();

        feedImageRepository.save(feedImage);
    }

    @Override
    public FeedDto.Like likeByMember(long feedId, long memberId) {
        Feed findFeed = methodFindByFeedId(feedId);
        Member findMember = methodFindByMemberId(memberId);
        Optional<FeedLike> optionalFeedLike = feedLikeRepository.findByMemberAndFeed(findMember,
                findFeed);
        FeedLike feedLike;
        if (optionalFeedLike.isEmpty()) {
            feedLike = FeedLike.builder()
                    .feed(findFeed)
                    .member(findMember)
                    .build();
            findFeed.likeCount(true);
        } else {
            feedLike = optionalFeedLike.get();
            feedLike.updateIsLike();
            findFeed.likeCount(feedLike.isLike());
        }
        FeedLike savedFeedLike = feedLikeRepository.save(feedLike);
        Feed savedFeed = feedRepository.save(findFeed);

        return FeedDto.Like.builder()
                .likeCount(savedFeed.getLikes())
                .isLike(savedFeedLike.isLike())
                .build();
    }

    //----------------------------------------------------------------------

    private Member methodFindByMemberId(long memberId) {
        return memberRepository.findById(memberId).orElseThrow(
                () -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    private Feed methodFindByFeedId(long feedId) {
        return feedRepository.findById(feedId).orElseThrow(
                () -> new RuntimeException("피드를 찾을 수 없습니다."));
    }

    private List<FeedCommentDto.Response> methodFindFeedCommentByFeedId(long feedId) {
        return feedCommentsRepository.findByFeedFeedId(feedId)
                .map(feedCommentsList -> {
                    List<FeedCommentDto.Response> feedCommentDtoList = new ArrayList<>();
                    for (FeedComments feedComments : feedCommentsList) {
                        FeedCommentDto.Response response = feedMapper.feedCommentsToFeedCommentDto(feedComments);
                        response.setMemberInfo(memberIdToMemberInfoDto(feedComments.getMember().getMemberId()));
                        feedCommentDtoList.add(response);
                    }
                    return feedCommentDtoList;
                })
                .orElseGet(Collections::emptyList);
    }

    private MemberDto.Info memberIdToMemberInfoDto(long memberId) {
        Member findMember = memberRepository.findById(memberId).orElseThrow(
                () -> new RuntimeException("사용자를 찾을 수 없습니다.")
        );

        return MemberDto.Info.builder()
                .memberId(findMember.getMemberId())
                .imageURL(findMember.getImageURL())
                .nickname(findMember.getNickname())
                .build();
    }


    private List<ImageDto> feedImageToImageDtoList(List<FeedImage> feedImageList) {
        List<ImageDto> imageDtoList = new ArrayList<>();
        for (FeedImage feedImage : feedImageList) {
            Image image = feedImage.getImage();
            imageDtoList.add(feedMapper.imageToImageDto(image));
        }
        return imageDtoList;
    }

    private boolean feedLikesByMember(Feed feed, Member member) {
        Optional<FeedLike> feedLike = feedLikeRepository.findByMemberAndFeed(member, feed);
        return feedLike.map(FeedLike::isLike).orElse(false);
    }





    private void addToRedisSet(List<Feed> values, long memberId) {
        for (Feed value : values) {
            String key = memberId + ":Feed";
            redisTemplate.opsForZSet().add(key, value.getFeedId().toString(), System.currentTimeMillis() + (3600 * 1000));
        }
    }

    private Set<String> getToRedis(long memberId) {
        long currentTime = System.currentTimeMillis();
        String key = memberId + ":Feed";
        Boolean result = redisTemplate.hasKey(key);
        if (result != null && result) {
            return redisTemplate.opsForZSet().rangeByScore(key, currentTime, Double.POSITIVE_INFINITY);
        } else {
            redisTemplate.expire(key, 2, TimeUnit.MINUTES);
            return redisTemplate.opsForZSet().rangeByScore(key, currentTime, Double.POSITIVE_INFINITY);
        }
    }

    public void deleteRedis(long memberId) {
        String key = memberId + ":Feed";
        redisTemplate.delete(key);
    }

    public FeedDtoList serviceDtoToFeedDtoList(FeedServiceDto.FeedListToServiceDto feedListToServiceDto) {
        List<FeedDto.Response> responseList = new ArrayList<>();
        for (Feed feed : feedListToServiceDto.getFeedList()) {
            FeedDto.Response response = changeFeedToFeedDtoResponse(feed.getFeedId(), feed.getMember().getMemberId());
            responseList.add(response);
        }
        return FeedDtoList.builder()
                .responseList(responseList)
                .build();
    }

    public FeedDtoList FollowFeedList(FeedDtoList list) {
        List<FeedDto.Response> responseList = new ArrayList<>();
        for (FeedDto.Response feedDto : list.getResponseList()) {
            long feedId = feedDto.getFeedId();
            Feed feed = methodFindByFeedId(feedId);
            FeedDto.Response response = changeFeedToFeedDtoResponse(feedDto.getFeedId(), feed.getMember().getMemberId());
            responseList.add(response);
        }

        return FeedDtoList.builder()
                .responseList(responseList)
                .build();
    }
}
