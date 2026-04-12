package com.bishe.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.JobDtos;
import com.bishe.recruitment.entity.CompanyProfile;
import com.bishe.recruitment.entity.JobPost;
import com.bishe.recruitment.enums.CompanyAuditStatus;
import com.bishe.recruitment.enums.JobStatus;
import com.bishe.recruitment.mapper.CompanyProfileMapper;
import com.bishe.recruitment.mapper.JobPostMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JobSearchServiceTests {

    private static final Long TITLE_COMPANY_USER_ID = 940001L;
    private static final Long COMPANY_NAME_USER_ID = 940002L;
    private static final Long FILTER_COMPANY_USER_ID = 940003L;
    private static final Long SORT_COMPANY_USER_ID = 940004L;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobPostMapper jobPostMapper;

    @Autowired
    private CompanyProfileMapper companyProfileMapper;

    @Test
    void searchJobs_matchesKeywordAgainstJobTitle() {
        createCompanyProfile(TITLE_COMPANY_USER_ID, "北辰数据科技");
        createPublishedJob(TITLE_COMPANY_USER_ID, "TEST-SEARCH-940001", "后端开发工程师",
                "后端开发", "上海", 15000, 22000, List.of("五险一金"), LocalDateTime.now().minusDays(1));
        createPublishedJob(TITLE_COMPANY_USER_ID, "TEST-SEARCH-940002", "测试工程师",
                "测试", "上海", 12000, 18000, List.of("带薪年假"), LocalDateTime.now().minusDays(2));

        JobDtos.JobSearchRequest request = new JobDtos.JobSearchRequest();
        request.setKeyword("后端");
        request.setPageSize(50L);

        PageResponse<Map<String, Object>> result = jobService.searchJobs(request, null);

        assertThat(extractJobCodes(result))
                .contains("TEST-SEARCH-940001")
                .doesNotContain("TEST-SEARCH-940002");
    }

    @Test
    void searchJobs_matchesKeywordAgainstCompanyName() {
        createCompanyProfile(COMPANY_NAME_USER_ID, "华星未来科技有限公司");
        createPublishedJob(COMPANY_NAME_USER_ID, "TEST-SEARCH-940003", "平台工程师",
                "平台开发", "杭州", 18000, 26000, List.of("五险一金"), LocalDateTime.now().minusDays(1));

        JobDtos.JobSearchRequest request = new JobDtos.JobSearchRequest();
        request.setKeyword("未来科技");
        request.setPageSize(50L);

        PageResponse<Map<String, Object>> result = jobService.searchJobs(request, null);

        assertThat(extractJobCodes(result)).contains("TEST-SEARCH-940003");
    }

    @Test
    void searchJobs_filtersByCurrentConditionsOnly() {
        createCompanyProfile(FILTER_COMPANY_USER_ID, "筛选测试企业");
        createPublishedJob(FILTER_COMPANY_USER_ID, "TEST-SEARCH-940004", "Java工程师",
                "后端开发", "上海", 18000, 26000, List.of("五险一金", "带薪年假"), LocalDateTime.now().minusDays(1));
        createPublishedJob(FILTER_COMPANY_USER_ID, "TEST-SEARCH-940005", "Java工程师",
                "后端开发", "北京", 18000, 26000, List.of("下午茶"), LocalDateTime.now().minusDays(2));

        JobDtos.JobSearchRequest request = new JobDtos.JobSearchRequest();
        request.setLocation("上海");
        request.setBenefitTags(List.of("五险一金"));
        request.setPageSize(50L);

        PageResponse<Map<String, Object>> result = jobService.searchJobs(request, null);

        assertThat(extractJobCodes(result))
                .contains("TEST-SEARCH-940004")
                .doesNotContain("TEST-SEARCH-940005");
    }

    @Test
    void searchJobs_returnsDefaultListSortedByPublishedAtDescWhenNoConditions() {
        createCompanyProfile(SORT_COMPANY_USER_ID, "默认排序企业");
        createPublishedJob(SORT_COMPANY_USER_ID, "TEST-SEARCH-940006", "旧岗位",
                "后端开发", "苏州", 12000, 18000, List.of("五险一金"), LocalDateTime.now().minusDays(5));
        createPublishedJob(SORT_COMPANY_USER_ID, "TEST-SEARCH-940007", "新岗位",
                "后端开发", "苏州", 12000, 18000, List.of("五险一金"), LocalDateTime.now().minusHours(6));

        JobDtos.JobSearchRequest request = new JobDtos.JobSearchRequest();
        request.setPageSize(50L);

        PageResponse<Map<String, Object>> result = jobService.searchJobs(request, null);
        List<String> jobCodes = extractJobCodes(result);

        assertThat(jobCodes).contains("TEST-SEARCH-940007", "TEST-SEARCH-940006");
        assertThat(jobCodes.indexOf("TEST-SEARCH-940007"))
                .isLessThan(jobCodes.indexOf("TEST-SEARCH-940006"));
    }

    @Test
    void searchJobs_supportsSalaryPrioritySorting() {
        createCompanyProfile(SORT_COMPANY_USER_ID + 10, "薪资排序企业");
        createPublishedJob(SORT_COMPANY_USER_ID + 10, "TEST-SEARCH-940008", "高薪岗位",
                "后端开发", "南京", 18000, 32000, List.of("五险一金"), LocalDateTime.now().minusDays(2));
        createPublishedJob(SORT_COMPANY_USER_ID + 10, "TEST-SEARCH-940009", "普通岗位",
                "后端开发", "南京", 15000, 22000, List.of("五险一金"), LocalDateTime.now().minusHours(4));

        JobDtos.JobSearchRequest request = new JobDtos.JobSearchRequest();
        request.setSortKey("salary");
        request.setPageSize(50L);

        PageResponse<Map<String, Object>> result = jobService.searchJobs(request, null);
        List<String> jobCodes = extractJobCodes(result);

        assertThat(jobCodes).contains("TEST-SEARCH-940008", "TEST-SEARCH-940009");
        assertThat(jobCodes.indexOf("TEST-SEARCH-940008"))
                .isLessThan(jobCodes.indexOf("TEST-SEARCH-940009"));
    }

    private List<String> extractJobCodes(PageResponse<Map<String, Object>> response) {
        return response.getRecords().stream()
                .map(item -> String.valueOf(item.get("jobCode")))
                .toList();
    }

    private void createCompanyProfile(Long userId, String companyName) {
        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(userId);
        profile.setCompanyName(companyName);
        profile.setUnifiedSocialCreditCode("USCC" + userId);
        profile.setContactPerson("Contact " + userId);
        profile.setPhone("138" + String.format("%08d", userId % 100000000L));
        profile.setEmail("company" + userId + "@example.com");
        profile.setAddress("Test address " + userId);
        profile.setDescription("Company profile for search tests.");
        profile.setAuditStatus(CompanyAuditStatus.APPROVED.name());
        companyProfileMapper.insert(profile);
    }

    private void createPublishedJob(Long companyUserId, String jobCode, String title, String category,
                                    String location, int salaryMin, int salaryMax,
                                    List<String> benefitTags, LocalDateTime publishedAt) {
        JobPost jobPost = new JobPost();
        jobPost.setCompanyUserId(companyUserId);
        jobPost.setJobCode(jobCode);
        jobPost.setTitle(title);
        jobPost.setCategory(category);
        jobPost.setLocation(location);
        jobPost.setSalaryMin(salaryMin);
        jobPost.setSalaryMax(salaryMax);
        jobPost.setExperienceRequirement("3年");
        jobPost.setEducationRequirement("本科");
        jobPost.setHeadcount(2);
        jobPost.setDescription("用于验证岗位列表筛选与排序逻辑的测试岗位。");
        jobPost.setBenefitTagsJson(toJson(benefitTags));
        jobPost.setStatus(JobStatus.PUBLISHED.name());
        jobPost.setPublishedAt(publishedAt);
        jobPost.setExpireAt(LocalDateTime.now().plusDays(10));
        jobPostMapper.insert(jobPost);
    }

    private String toJson(List<String> items) {
        return items.stream()
                .map(item -> "\"" + item + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
