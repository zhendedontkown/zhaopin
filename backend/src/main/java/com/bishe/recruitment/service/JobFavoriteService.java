package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.entity.JobFavorite;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.mapper.JobFavoriteMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobFavoriteService {

    private final JobFavoriteMapper jobFavoriteMapper;
    private final JobPostMapper jobPostMapper;
    private final JobService jobService;

    public JobFavoriteService(JobFavoriteMapper jobFavoriteMapper, JobPostMapper jobPostMapper, JobService jobService) {
        this.jobFavoriteMapper = jobFavoriteMapper;
        this.jobPostMapper = jobPostMapper;
        this.jobService = jobService;
    }

    @Transactional
    public Map<String, Object> favoriteJob(Long jobseekerUserId, Long jobId) {
        JobPost jobPost = requireFavoritableJob(jobId);
        JobFavorite existingFavorite = findFavorite(jobseekerUserId, jobId);
        if (existingFavorite == null) {
            JobFavorite favorite = new JobFavorite();
            favorite.setJobId(jobId);
            favorite.setJobseekerUserId(jobseekerUserId);
            jobFavoriteMapper.insert(favorite);
        }
        return jobService.buildJobViewForUser(jobPost, jobseekerUserId);
    }

    @Transactional
    public Map<String, Object> unfavoriteJob(Long jobseekerUserId, Long jobId) {
        jobFavoriteMapper.delete(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getJobId, jobId)
                .eq(JobFavorite::getJobseekerUserId, jobseekerUserId));
        JobPost jobPost = jobPostMapper.selectById(jobId);
        if (jobPost == null) {
            return Map.of("jobId", jobId, "favorited", false);
        }
        return jobService.buildJobViewForUser(jobPost, jobseekerUserId);
    }

    public PageResponse<Map<String, Object>> listFavorites(Long jobseekerUserId, long pageNum, long pageSize) {
        List<JobFavorite> favorites = jobFavoriteMapper.selectList(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getJobseekerUserId, jobseekerUserId)
                .orderByDesc(JobFavorite::getCreatedAt));
        if (favorites.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        List<Long> jobIds = favorites.stream().map(JobFavorite::getJobId).toList();
        Map<Long, JobPost> jobMap = jobPostMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(JobPost::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<JobPost> visibleJobs = jobIds.stream()
                .map(jobMap::get)
                .filter(Objects::nonNull)
                .filter(job -> !Integer.valueOf(1).equals(job.getDeletedFlag()))
                .toList();
        if (visibleJobs.isEmpty()) {
            return emptyPage(pageNum, pageSize);
        }

        List<Map<String, Object>> views = jobService.buildJobViewsForUser(visibleJobs, jobseekerUserId);
        long current = pageNum < 1 ? 1 : pageNum;
        long size = pageSize < 1 ? 10 : pageSize;
        int fromIndex = (int) ((current - 1) * size);
        if (fromIndex >= views.size()) {
            return PageResponse.<Map<String, Object>>builder()
                    .pageNum(current)
                    .pageSize(size)
                    .total(views.size())
                    .records(List.of())
                    .build();
        }
        int toIndex = Math.min(views.size(), fromIndex + (int) size);
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(current)
                .pageSize(size)
                .total(views.size())
                .records(views.subList(fromIndex, toIndex))
                .build();
    }

    private PageResponse<Map<String, Object>> emptyPage(long pageNum, long pageSize) {
        return PageResponse.<Map<String, Object>>builder()
                .pageNum(pageNum < 1 ? 1 : pageNum)
                .pageSize(pageSize < 1 ? 10 : pageSize)
                .total(0)
                .records(List.of())
                .build();
    }

    private JobFavorite findFavorite(Long jobseekerUserId, Long jobId) {
        return jobFavoriteMapper.selectOne(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getJobId, jobId)
                .eq(JobFavorite::getJobseekerUserId, jobseekerUserId)
                .last("LIMIT 1"));
    }

    private JobPost requireFavoritableJob(Long jobId) {
        JobPost jobPost = jobPostMapper.selectById(jobId);
        if (jobPost == null) {
            throw new BusinessException("岗位不存在");
        }
        if (Integer.valueOf(1).equals(jobPost.getDeletedFlag())) {
            throw new BusinessException("岗位不存在");
        }
        if (!JobStatus.PUBLISHED.name().equals(jobPost.getStatus())) {
            throw new BusinessException("该岗位暂不支持收藏");
        }
        if (jobPost.getExpireAt() != null && jobPost.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("该岗位已过期，暂不支持收藏");
        }
        return jobPost;
    }
}
