package com.bishe.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bishe.recruitment.dto.AuthDtos;
import com.bishe.recruitment.entity.JobFavorite;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.mapper.JobFavoriteMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JobFavoriteServiceTests {

    @Autowired
    private JobFavoriteService jobFavoriteService;

    @Autowired
    private JobFavoriteMapper jobFavoriteMapper;

    @Autowired
    private JobPostMapper jobPostMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void favoriteAndUnfavoriteAreIdempotentAndListFavoritesReturnsVisibleJobs() {
        Long jobseekerUserId = registerJobseeker("favorite.user@example.com", "13700003001");
        JobPost firstJob = createPublishedJob(940001L, "TEST-FAVORITE-001", "收藏测试岗位一");
        JobPost secondJob = createPublishedJob(940002L, "TEST-FAVORITE-002", "收藏测试岗位二");

        Map<String, Object> firstFavoriteView = jobFavoriteService.favoriteJob(jobseekerUserId, firstJob.getId());
        jobFavoriteService.favoriteJob(jobseekerUserId, firstJob.getId());
        jobFavoriteService.favoriteJob(jobseekerUserId, secondJob.getId());

        long duplicateCount = jobFavoriteMapper.selectCount(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getJobId, firstJob.getId())
                .eq(JobFavorite::getJobseekerUserId, jobseekerUserId));
        assertThat(duplicateCount).isEqualTo(1);
        assertThat(firstFavoriteView.get("favorited")).isEqualTo(true);

        JobFavorite firstFavorite = jobFavoriteMapper.selectOne(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getJobId, firstJob.getId())
                .eq(JobFavorite::getJobseekerUserId, jobseekerUserId));
        JobFavorite secondFavorite = jobFavoriteMapper.selectOne(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getJobId, secondJob.getId())
                .eq(JobFavorite::getJobseekerUserId, jobseekerUserId));
        firstFavorite.setCreatedAt(LocalDateTime.now().minusDays(1));
        jobFavoriteMapper.updateById(firstFavorite);
        secondFavorite.setCreatedAt(LocalDateTime.now());
        jobFavoriteMapper.updateById(secondFavorite);

        Map<String, Object> favoritePage = pageToMap(jobFavoriteService.listFavorites(jobseekerUserId, 1, 5));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) favoritePage.get("records");
        assertThat(favoritePage.get("total")).isEqualTo(2L);
        assertThat(records).hasSize(2);
        assertThat(records.get(0).get("id")).isEqualTo(secondJob.getId());
        assertThat(records.get(1).get("id")).isEqualTo(firstJob.getId());
        assertThat(records).allMatch(record -> Boolean.TRUE.equals(record.get("favorited")));

        Map<String, Object> unfavoriteView = jobFavoriteService.unfavoriteJob(jobseekerUserId, firstJob.getId());
        jobFavoriteService.unfavoriteJob(jobseekerUserId, firstJob.getId());

        long remainingCount = jobFavoriteMapper.selectCount(new LambdaQueryWrapper<JobFavorite>()
                .eq(JobFavorite::getJobId, firstJob.getId())
                .eq(JobFavorite::getJobseekerUserId, jobseekerUserId));
        assertThat(remainingCount).isZero();
        assertThat(unfavoriteView.get("favorited")).isEqualTo(false);
    }

    @Test
    void favoriteEndpointsAllowJobseekersAndRejectCompanies() throws Exception {
        Long jobseekerUserId = registerJobseeker("favorite.auth.user@example.com", "13700003002");
        registerCompany("favorite.auth.company@example.com", "13800003002", "91310000MA1K883002");
        String jobseekerToken = login("favorite.auth.user@example.com", "Password123");
        String companyToken = login("favorite.auth.company@example.com", "Password123");

        mockMvc.perform(get("/api/jobseeker/favorites")
                        .header("Authorization", "Bearer " + jobseekerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber());

        mockMvc.perform(get("/api/jobseeker/favorites")
                        .header("Authorization", "Bearer " + companyToken))
                .andExpect(status().isForbidden());

        assertThat(jobseekerUserId).isNotNull();
    }

    private Long registerJobseeker(String email, String phone) {
        AuthDtos.JobseekerRegisterRequest request = new AuthDtos.JobseekerRegisterRequest();
        request.setFullName("收藏测试求职者");
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword("Password123");
        return ((Number) authService.registerJobseeker(request).get("userId")).longValue();
    }

    private void registerCompany(String email, String phone, String creditCode) {
        AuthDtos.CompanyRegisterRequest request = new AuthDtos.CompanyRegisterRequest();
        request.setCompanyName("收藏测试企业");
        request.setUnifiedSocialCreditCode(creditCode);
        request.setContactPerson("测试联系人");
        request.setPhone(phone);
        request.setEmail(email);
        request.setPassword("Password123");
        authService.registerCompany(request);
    }

    private String login(String account, String password) {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest();
        request.setAccount(account);
        request.setPassword(password);
        return authService.login(request).getToken();
    }

    private Map<String, Object> pageToMap(com.bishe.recruitment.common.PageResponse<Map<String, Object>> page) {
        return Map.of(
                "pageNum", page.getPageNum(),
                "pageSize", page.getPageSize(),
                "total", page.getTotal(),
                "records", page.getRecords());
    }

    private JobPost createPublishedJob(Long companyUserId, String jobCode, String title) {
        JobPost jobPost = new JobPost();
        jobPost.setCompanyUserId(companyUserId);
        jobPost.setJobCode(jobCode);
        jobPost.setTitle(title);
        jobPost.setCategory("后端开发");
        jobPost.setLocation("上海");
        jobPost.setSalaryMin(10000);
        jobPost.setSalaryMax(18000);
        jobPost.setExperienceRequirement("3年");
        jobPost.setEducationRequirement("本科");
        jobPost.setHeadcount(1);
        jobPost.setDescription("这是一条用于收藏能力测试的岗位描述，长度足够覆盖详情展示和收藏链路验证。");
        jobPost.setStatus(JobStatus.PUBLISHED.name());
        jobPost.setPublishedAt(LocalDateTime.now().minusDays(1));
        jobPost.setExpireAt(LocalDateTime.now().plusDays(7));
        jobPostMapper.insert(jobPost);
        return jobPost;
    }
}
