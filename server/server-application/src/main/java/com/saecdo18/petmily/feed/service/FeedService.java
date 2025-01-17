package com.saecdo18.petmily.feed.service;

import com.saecdo18.petmily.feed.dto.FeedDto;
import com.saecdo18.petmily.feed.dto.FeedDtoList;
import com.saecdo18.petmily.feed.dto.FeedServiceDto;
import com.saecdo18.petmily.feed.entity.Feed;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface FeedService {

    public Long createFeed(FeedServiceDto.Post post, long memberId) throws IOException;

//    public FeedDto.Response getFeed(long feedId, long memberId);

    public FeedServiceDto.FeedListToServiceDto getFeedsRecent(long memberId, int page, int size);

    public FeedServiceDto.FeedListToServiceDto getFeedsByMember(int page, int size, long memberId);

    public FeedServiceDto.FeedListToServiceDto getFeedsByMemberFollow(long memberId, int page, int size);

    public Long patchFeed(FeedServiceDto.Patch patch, long memberId) throws IOException;

    public void deleteFeed(long feedId, long memberId);

    public void saveImage(Feed feed, String originalFilename, String uploadFileURL);

    public FeedDto.Like likeByMember(long feedId, long memberId);
}
