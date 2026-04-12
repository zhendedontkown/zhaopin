package com.bishe.recruitment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.recruitment.common.BusinessException;
import com.bishe.recruitment.common.PageResponse;
import com.bishe.recruitment.dto.ResumeDtos;
import com.bishe.recruitment.entity.JobseekerProfile;
import com.bishe.recruitment.entity.Resume;
import com.bishe.recruitment.entity.ResumeEducation;
import com.bishe.recruitment.entity.ResumeExperience;
import com.bishe.recruitment.entity.ResumeExtraSectionItem;
import com.bishe.recruitment.entity.ResumeProject;
import com.bishe.recruitment.entity.ResumeSkill;
import com.bishe.recruitment.entity.SavedResume;
import com.bishe.recruitment.entity.SkillDict;
import com.bishe.recruitment.mapper.JobseekerProfileMapper;
import com.bishe.recruitment.mapper.ResumeEducationMapper;
import com.bishe.recruitment.mapper.ResumeExperienceMapper;
import com.bishe.recruitment.mapper.ResumeExtraSectionItemMapper;
import com.bishe.recruitment.mapper.ResumeMapper;
import com.bishe.recruitment.mapper.ResumeProjectMapper;
import com.bishe.recruitment.mapper.ResumeSkillMapper;
import com.bishe.recruitment.mapper.SavedResumeMapper;
import com.bishe.recruitment.mapper.SkillDictMapper;
import com.bishe.recruitment.util.ResumeModuleConfigSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ResumeService {

    private static final String SECTION_INTERNSHIP = "INTERNSHIP";
    private static final String SECTION_CAMPUS = "CAMPUS";
    private static final String SECTION_HONOR = "HONOR";
    private static final String SECTION_HOBBY = "HOBBY";
    private static final String SECTION_CUSTOM = "CUSTOM";

    private final ResumeMapper resumeMapper;
    private final ResumeEducationMapper resumeEducationMapper;
    private final ResumeExperienceMapper resumeExperienceMapper;
    private final ResumeProjectMapper resumeProjectMapper;
    private final ResumeExtraSectionItemMapper resumeExtraSectionItemMapper;
    private final ResumeSkillMapper resumeSkillMapper;
    private final SavedResumeMapper savedResumeMapper;
    private final SkillDictMapper skillDictMapper;
    private final JobseekerProfileMapper jobseekerProfileMapper;
    private final ObjectMapper objectMapper;

    public ResumeService(ResumeMapper resumeMapper, ResumeEducationMapper resumeEducationMapper,
                         ResumeExperienceMapper resumeExperienceMapper, ResumeProjectMapper resumeProjectMapper,
                         ResumeExtraSectionItemMapper resumeExtraSectionItemMapper, ResumeSkillMapper resumeSkillMapper,
                         SavedResumeMapper savedResumeMapper, SkillDictMapper skillDictMapper,
                         JobseekerProfileMapper jobseekerProfileMapper, ObjectMapper objectMapper) {
        this.resumeMapper = resumeMapper;
        this.resumeEducationMapper = resumeEducationMapper;
        this.resumeExperienceMapper = resumeExperienceMapper;
        this.resumeProjectMapper = resumeProjectMapper;
        this.resumeExtraSectionItemMapper = resumeExtraSectionItemMapper;
        this.resumeSkillMapper = resumeSkillMapper;
        this.savedResumeMapper = savedResumeMapper;
        this.skillDictMapper = skillDictMapper;
        this.jobseekerProfileMapper = jobseekerProfileMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getResumeDetail(Long userId) {
        Resume resume = getOrCreateByUserId(userId);
        return buildResumeDetail(resume);
    }

    public Map<String, Object> getResumeDetailByResumeId(Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BusinessException("简历不存在");
        }
        return buildResumeDetail(resume);
    }

    public PageResponse<ResumeDtos.SavedResumeSummaryView> listSavedResumes(Long userId, boolean completeOnly,
                                                                            long pageNum, long pageSize) {
        Page<SavedResume> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SavedResume> wrapper = new LambdaQueryWrapper<SavedResume>()
                .eq(SavedResume::getUserId, userId)
                .orderByDesc(SavedResume::getUpdatedAt)
                .orderByDesc(SavedResume::getId);
        if (completeOnly) {
            wrapper.eq(SavedResume::getCompleteFlag, true);
        }
        Page<SavedResume> result = savedResumeMapper.selectPage(page, wrapper);
        List<ResumeDtos.SavedResumeSummaryView> records = result.getRecords().stream()
                .map(this::toSavedResumeSummaryView)
                .toList();
        return PageResponse.<ResumeDtos.SavedResumeSummaryView>builder()
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .total(result.getTotal())
                .records(records)
                .build();
    }

    public ResumeDtos.SavedResumeDetailView getSavedResumeDetail(Long userId, Long savedResumeId) {
        SavedResume savedResume = getOwnedSavedResumeOrThrow(userId, savedResumeId);
        ResumeDtos.SavedResumeDetailView view = new ResumeDtos.SavedResumeDetailView();
        view.setId(savedResume.getId());
        view.setName(savedResume.getName());
        view.setTemplateCode(savedResume.getTemplateCode());
        view.setCompletenessScore(savedResume.getCompletenessScore());
        view.setCompleteFlag(Boolean.TRUE.equals(savedResume.getCompleteFlag()));
        view.setMissingItems(readStringListJson(savedResume.getMissingItemsJson()));
        view.setResumeDetail(parseResumeSnapshotJson(savedResume.getSnapshotJson()));
        view.setCreatedAt(savedResume.getCreatedAt());
        view.setUpdatedAt(savedResume.getUpdatedAt());
        return view;
    }

    public SavedResume getCompleteSavedResumeOrThrow(Long userId, Long savedResumeId) {
        SavedResume savedResume = getOwnedSavedResumeOrThrow(userId, savedResumeId);
        if (!Boolean.TRUE.equals(savedResume.getCompleteFlag())) {
            throw new BusinessException("所选简历尚未完善，暂时无法投递");
        }
        return savedResume;
    }

    public String createResumeSnapshotJson(Long resumeId) {
        try {
            return objectMapper.writeValueAsString(getResumeDetailByResumeId(resumeId));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法序列化简历快照", ex);
        }
    }

    public String createResumeSnapshotJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法序列化简历快照", ex);
        }
    }

    public Map<String, Object> parseResumeSnapshotJson(String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException ex) {
            throw new BusinessException("简历快照解析失败");
        }
    }

    public Resume getOrCreateByUserId(Long userId) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>().eq(Resume::getUserId, userId));
        if (resume == null) {
            resume = new Resume();
            resume.setUserId(userId);
            resume.setTemplateCode("classic");
            resume.setModuleConfigJson(writeModuleConfig(normalizeModuleConfig(List.of())));
            resume.setCompletenessScore(0);
            resumeMapper.insert(resume);
        } else if (!StringUtils.hasText(resume.getModuleConfigJson())) {
            resume.setModuleConfigJson(writeModuleConfig(normalizeModuleConfig(List.of())));
            resumeMapper.updateById(resume);
        }
        return resume;
    }

    @Transactional
    public Map<String, Object> saveResume(Long userId, ResumeDtos.ResumeSaveRequest request) {
        Resume resume = getOrCreateByUserId(userId);
        List<ResumeDtos.ModuleConfigItem> moduleConfig = normalizeModuleConfig(request.getModuleConfig());

        resume.setTemplateCode(request.getTemplateCode());
        resume.setModuleConfigJson(writeModuleConfig(moduleConfig));
        resume.setFullName(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getFullName() : null);
        resume.setGender(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getGender() : null);
        resume.setBirthDate(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getBirthDate() : null);
        resume.setDisplayAge(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? Boolean.TRUE.equals(request.getDisplayAge()) : Boolean.TRUE);
        resume.setAge(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? calculateAge(request.getBirthDate()) : null);
        resume.setPhone(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getPhone() : null);
        resume.setEmail(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getEmail() : null);
        resume.setCity(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getCity() : null);
        resume.setHighestEducation(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getHighestEducation() : null);
        resume.setYearsOfExperience(isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) ? request.getYearsOfExperience() : null);
        resume.setSummary(isVisible(moduleConfig, ResumeModuleConfigSupport.SELF_EVALUATION) ? request.getSummary() : null);
        resume.setExpectedCategory(isVisible(moduleConfig, ResumeModuleConfigSupport.JOB_INTENT) ? request.getExpectedCategory() : null);
        resume.setExpectedSalaryMin(isVisible(moduleConfig, ResumeModuleConfigSupport.JOB_INTENT) ? request.getExpectedSalaryMin() : null);
        resume.setExpectedSalaryMax(isVisible(moduleConfig, ResumeModuleConfigSupport.JOB_INTENT) ? request.getExpectedSalaryMax() : null);
        resumeMapper.updateById(resume);

        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.EDUCATION),
                () -> replaceEducations(resume.getId(), request.getEducations()),
                () -> clearEducations(resume.getId()));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.WORK_EXPERIENCE),
                () -> replaceExperiences(resume.getId(), request.getExperiences()),
                () -> clearExperiences(resume.getId()));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.PROJECT_EXPERIENCE),
                () -> replaceProjects(resume.getId(), request.getProjects()),
                () -> clearProjects(resume.getId()));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.INTERNSHIP),
                () -> replaceExtraSectionItems(resume.getId(), SECTION_INTERNSHIP, request.getInternships()),
                () -> clearExtraSectionItems(resume.getId(), SECTION_INTERNSHIP));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.CAMPUS),
                () -> replaceExtraSectionItems(resume.getId(), SECTION_CAMPUS, request.getCampusExperiences()),
                () -> clearExtraSectionItems(resume.getId(), SECTION_CAMPUS));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.HONORS),
                () -> replaceExtraSectionItems(resume.getId(), SECTION_HONOR, request.getHonors()),
                () -> clearExtraSectionItems(resume.getId(), SECTION_HONOR));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.HOBBIES),
                () -> replaceExtraSectionItems(resume.getId(), SECTION_HOBBY, request.getHobbies()),
                () -> clearExtraSectionItems(resume.getId(), SECTION_HOBBY));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.CUSTOM_FIELDS),
                () -> replaceCustomFields(resume.getId(), request.getCustomFields()),
                () -> clearExtraSectionItems(resume.getId(), SECTION_CUSTOM));
        syncSection(isVisible(moduleConfig, ResumeModuleConfigSupport.SKILLS),
                () -> replaceSkills(resume.getId(), request.getSkills()),
                () -> clearSkills(resume.getId()));

        Map<String, Object> detail = buildResumeDetail(resume);
        resume.setCompletenessScore(calculateCompleteness(detail));
        resumeMapper.updateById(resume);
        syncProfile(userId, resume);
        return buildResumeDetail(resume);
    }

    @Transactional
    public ResumeDtos.CreateSavedResumeResponse createSavedResume(Long userId, ResumeDtos.CreateSavedResumeRequest request) {
        String normalizedName = normalizeSavedResumeName(request.getName());
        ensureSavedResumeNameAvailable(userId, normalizedName, null);

        Map<String, Object> currentDraft = saveResume(userId, request.getDraft());
        SavedResume savedResume = buildSavedResumeEntity(userId, normalizedName, request.getDraft().getTemplateCode(), currentDraft);
        savedResumeMapper.insert(savedResume);

        ResumeDtos.CreateSavedResumeResponse response = new ResumeDtos.CreateSavedResumeResponse();
        response.setSavedResume(toSavedResumeSummaryView(savedResume));
        response.setCurrentDraft(currentDraft);
        return response;
    }

    @Transactional
    public ResumeDtos.UpdateSavedResumeResponse updateSavedResume(Long userId, Long savedResumeId,
                                                                 ResumeDtos.UpdateSavedResumeRequest request) {
        String normalizedName = normalizeSavedResumeName(request.getName());
        SavedResume savedResume = getOwnedSavedResumeOrThrow(userId, savedResumeId);
        ensureSavedResumeNameAvailable(userId, normalizedName, savedResumeId);

        Map<String, Object> currentDraft = saveResume(userId, request.getDraft());
        populateSavedResumeEntity(savedResume, normalizedName, request.getDraft().getTemplateCode(), currentDraft);
        savedResumeMapper.updateById(savedResume);

        ResumeDtos.UpdateSavedResumeResponse response = new ResumeDtos.UpdateSavedResumeResponse();
        response.setSavedResume(toSavedResumeSummaryView(savedResume));
        response.setCurrentDraft(currentDraft);
        return response;
    }

    @Transactional
    public void deleteSavedResume(Long userId, Long savedResumeId) {
        SavedResume savedResume = getOwnedSavedResumeOrThrow(userId, savedResumeId);
        savedResumeMapper.deleteById(savedResume.getId());
    }

    public Resume validateCompleteOrThrow(Long userId) {
        Resume resume = getOrCreateByUserId(userId);
        Map<String, Object> detail = buildResumeDetail(resume);
        @SuppressWarnings("unchecked")
        List<String> missingItems = (List<String>) detail.get("missingItems");
        if (!missingItems.isEmpty()) {
            throw new BusinessException("简历不完整，请先完善以下内容: " + String.join("、", missingItems));
        }
        return resume;
    }

    public List<String> suggestSkills(String keyword) {
        LambdaQueryWrapper<SkillDict> wrapper = new LambdaQueryWrapper<SkillDict>()
                .orderByAsc(SkillDict::getSkillName)
                .last("limit 10");
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SkillDict::getSkillName, keyword);
        }
        return skillDictMapper.selectList(wrapper).stream().map(SkillDict::getSkillName).toList();
    }

    public List<String> getResumeSkills(Long resumeId) {
        return resumeSkillMapper.selectList(new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId))
                .stream().map(ResumeSkill::getSkillName).toList();
    }

    private Map<String, Object> buildResumeDetail(Resume resume) {
        List<ResumeDtos.ModuleConfigItem> moduleConfig = readModuleConfig(resume.getModuleConfigJson());
        List<ResumeDtos.EducationItem> educations = toEducationItems(resumeEducationMapper.selectList(new LambdaQueryWrapper<ResumeEducation>()
                .eq(ResumeEducation::getResumeId, resume.getId()).orderByAsc(ResumeEducation::getSortOrder)));
        List<ResumeDtos.ExperienceItem> experiences = toExperienceItems(resumeExperienceMapper.selectList(new LambdaQueryWrapper<ResumeExperience>()
                .eq(ResumeExperience::getResumeId, resume.getId()).orderByAsc(ResumeExperience::getSortOrder)));
        List<ResumeDtos.ProjectItem> projects = toProjectItems(resumeProjectMapper.selectList(new LambdaQueryWrapper<ResumeProject>()
                .eq(ResumeProject::getResumeId, resume.getId()).orderByAsc(ResumeProject::getSortOrder)));
        List<ResumeDtos.ExtraSectionItem> internships = toExtraSectionItems(listExtraSectionItems(resume.getId(), SECTION_INTERNSHIP));
        List<ResumeDtos.ExtraSectionItem> campusExperiences = toExtraSectionItems(listExtraSectionItems(resume.getId(), SECTION_CAMPUS));
        List<ResumeDtos.ExtraSectionItem> honors = toExtraSectionItems(listExtraSectionItems(resume.getId(), SECTION_HONOR));
        List<ResumeDtos.ExtraSectionItem> hobbies = toExtraSectionItems(listExtraSectionItems(resume.getId(), SECTION_HOBBY));
        List<ResumeDtos.CustomFieldItem> customFields = toCustomFieldItems(listExtraSectionItems(resume.getId(), SECTION_CUSTOM));
        List<String> skills = getResumeSkills(resume.getId());

        List<String> missingItems = new ArrayList<>();
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && !StringUtils.hasText(resume.getFullName())) {
            missingItems.add("姓名");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && !StringUtils.hasText(resume.getGender())) {
            missingItems.add("性别");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && resume.getBirthDate() == null) {
            missingItems.add("出生年月");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && !StringUtils.hasText(resume.getPhone())) {
            missingItems.add("联系电话");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && !StringUtils.hasText(resume.getEmail())) {
            missingItems.add("联系邮箱");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && !StringUtils.hasText(resume.getCity())) {
            missingItems.add("籍贯/城市");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && resume.getYearsOfExperience() == null) {
            missingItems.add("工作年限");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.EDUCATION) && educations.isEmpty()) {
            missingItems.add("教育经历");
        }
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.SKILLS) && skills.isEmpty()) {
            missingItems.add("技能标签");
        }

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("resume", resume);
        result.put("moduleConfig", moduleConfig);
        result.put("educations", educations);
        result.put("experiences", experiences);
        result.put("projects", projects);
        result.put("internships", internships);
        result.put("campusExperiences", campusExperiences);
        result.put("honors", honors);
        result.put("hobbies", hobbies);
        result.put("customFields", customFields);
        result.put("skills", skills);
        result.put("missingItems", missingItems);
        result.put("completenessScore", calculateCompleteness(result));
        return result;
    }

    private int calculateCompleteness(Map<String, Object> detail) {
        Resume resume = (Resume) detail.get("resume");
        @SuppressWarnings("unchecked")
        List<ResumeDtos.ModuleConfigItem> moduleConfig = (List<ResumeDtos.ModuleConfigItem>) detail.get("moduleConfig");
        @SuppressWarnings("unchecked")
        List<ResumeDtos.EducationItem> educations = (List<ResumeDtos.EducationItem>) detail.get("educations");
        @SuppressWarnings("unchecked")
        List<ResumeDtos.ExperienceItem> experiences = (List<ResumeDtos.ExperienceItem>) detail.get("experiences");
        @SuppressWarnings("unchecked")
        List<ResumeDtos.ProjectItem> projects = (List<ResumeDtos.ProjectItem>) detail.get("projects");
        @SuppressWarnings("unchecked")
        List<ResumeDtos.ExtraSectionItem> internships = (List<ResumeDtos.ExtraSectionItem>) detail.get("internships");
        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) detail.get("skills");
        int score = 0;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO) && StringUtils.hasText(resume.getFullName())) score += 15;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.BASIC_INFO)
                && StringUtils.hasText(resume.getGender())
                && resume.getBirthDate() != null
                && StringUtils.hasText(resume.getPhone())
                && StringUtils.hasText(resume.getEmail())
                && StringUtils.hasText(resume.getCity())
                && resume.getYearsOfExperience() != null) score += 15;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.JOB_INTENT)
                && (StringUtils.hasText(resume.getExpectedCategory())
                || resume.getExpectedSalaryMin() != null || resume.getExpectedSalaryMax() != null)) score += 10;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.EDUCATION) && !educations.isEmpty()) score += 20;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.WORK_EXPERIENCE) && !experiences.isEmpty()) score += 15;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.PROJECT_EXPERIENCE) && !projects.isEmpty()) score += 10;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.INTERNSHIP) && !internships.isEmpty()) score += 5;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.SKILLS) && !skills.isEmpty()) score += 15;
        if (isVisible(moduleConfig, ResumeModuleConfigSupport.SELF_EVALUATION) && StringUtils.hasText(resume.getSummary())) score += 10;
        return Math.min(score, 100);
    }

    private void replaceEducations(Long resumeId, List<ResumeDtos.EducationItem> items) {
        clearEducations(resumeId);
        List<ResumeDtos.EducationItem> safeItems = items == null ? List.of() : items;
        for (int i = 0; i < safeItems.size(); i++) {
            ResumeDtos.EducationItem item = safeItems.get(i);
            if (isEducationItemBlank(item)) continue;
            validateEducationItem(item, i);
            ResumeEducation entity = new ResumeEducation();
            entity.setResumeId(resumeId);
            entity.setSchoolName(item.getSchoolName());
            entity.setMajor(item.getMajor());
            entity.setDegree(item.getDegree());
            entity.setStartDate(item.getStartDate());
            entity.setEndDate(Boolean.TRUE.equals(item.getCurrent()) ? null : item.getEndDate());
            entity.setCurrentFlag(Boolean.TRUE.equals(item.getCurrent()));
            entity.setDescription(item.getDescription());
            entity.setSortOrder(item.getSortOrder() == null ? i : item.getSortOrder());
            resumeEducationMapper.insert(entity);
        }
    }

    private void replaceExperiences(Long resumeId, List<ResumeDtos.ExperienceItem> items) {
        clearExperiences(resumeId);
        List<ResumeDtos.ExperienceItem> safeItems = items == null ? List.of() : items;
        for (int i = 0; i < safeItems.size(); i++) {
            ResumeDtos.ExperienceItem item = safeItems.get(i);
            if (isExperienceItemBlank(item)) continue;
            validateExperienceItem(item, i);
            ResumeExperience entity = new ResumeExperience();
            entity.setResumeId(resumeId);
            entity.setCompanyName(item.getCompanyName());
            entity.setJobTitle(item.getJobTitle());
            entity.setStartDate(item.getStartDate());
            entity.setEndDate(Boolean.TRUE.equals(item.getCurrent()) ? null : item.getEndDate());
            entity.setCurrentFlag(Boolean.TRUE.equals(item.getCurrent()));
            entity.setDescription(item.getDescription());
            entity.setSortOrder(item.getSortOrder() == null ? i : item.getSortOrder());
            resumeExperienceMapper.insert(entity);
        }
    }

    private void replaceProjects(Long resumeId, List<ResumeDtos.ProjectItem> items) {
        clearProjects(resumeId);
        List<ResumeDtos.ProjectItem> safeItems = items == null ? List.of() : items;
        for (int i = 0; i < safeItems.size(); i++) {
            ResumeDtos.ProjectItem item = safeItems.get(i);
            if (isProjectItemBlank(item)) continue;
            validateProjectItem(item, i);
            ResumeProject entity = new ResumeProject();
            entity.setResumeId(resumeId);
            entity.setProjectName(item.getProjectName());
            entity.setRoleName(item.getRoleName());
            entity.setStartDate(item.getStartDate());
            entity.setEndDate(Boolean.TRUE.equals(item.getCurrent()) ? null : item.getEndDate());
            entity.setCurrentFlag(Boolean.TRUE.equals(item.getCurrent()));
            entity.setDescription(item.getDescription());
            entity.setSortOrder(item.getSortOrder() == null ? i : item.getSortOrder());
            resumeProjectMapper.insert(entity);
        }
    }

    private void replaceExtraSectionItems(Long resumeId, String sectionCode, List<ResumeDtos.ExtraSectionItem> items) {
        clearExtraSectionItems(resumeId, sectionCode);
        List<ResumeDtos.ExtraSectionItem> safeItems = items == null ? List.of() : items;
        for (int i = 0; i < safeItems.size(); i++) {
            ResumeDtos.ExtraSectionItem item = safeItems.get(i);
            if (isExtraSectionItemBlank(item)) continue;
            validateExtraSectionItem(item, i, sectionCode);
            boolean supportsCurrent = supportsCurrentFlag(sectionCode);
            ResumeExtraSectionItem entity = new ResumeExtraSectionItem();
            entity.setResumeId(resumeId);
            entity.setSectionCode(sectionCode);
            entity.setTitle(item.getTitle());
            entity.setSubtitle(item.getSubtitle());
            entity.setStartDate(item.getStartDate());
            entity.setEndDate(supportsCurrent && Boolean.TRUE.equals(item.getCurrent()) ? null : item.getEndDate());
            entity.setCurrentFlag(supportsCurrent && Boolean.TRUE.equals(item.getCurrent()));
            entity.setDescription(item.getDescription());
            entity.setSortOrder(item.getSortOrder() == null ? i : item.getSortOrder());
            resumeExtraSectionItemMapper.insert(entity);
        }
    }

    private void replaceCustomFields(Long resumeId, List<ResumeDtos.CustomFieldItem> items) {
        clearExtraSectionItems(resumeId, SECTION_CUSTOM);
        List<ResumeDtos.CustomFieldItem> safeItems = items == null ? List.of() : items;
        for (int i = 0; i < safeItems.size(); i++) {
            ResumeDtos.CustomFieldItem item = safeItems.get(i);
            if (!StringUtils.hasText(item.getKey()) && !StringUtils.hasText(item.getValue())) continue;
            if (!StringUtils.hasText(item.getKey()) || !StringUtils.hasText(item.getValue())) {
                throw new BusinessException("自定义信息第" + (i + 1) + "条请同时填写名称和内容");
            }
            ResumeExtraSectionItem entity = new ResumeExtraSectionItem();
            entity.setResumeId(resumeId);
            entity.setSectionCode(SECTION_CUSTOM);
            entity.setTitle(item.getKey());
            entity.setSubtitle(item.getValue());
            entity.setSortOrder(item.getSortOrder() == null ? i : item.getSortOrder());
            resumeExtraSectionItemMapper.insert(entity);
        }
    }

    private void replaceSkills(Long resumeId, List<String> skills) {
        clearSkills(resumeId);
        for (String skillName : skills == null ? List.<String>of() : skills) {
            if (!StringUtils.hasText(skillName)) continue;
            SkillDict skillDict = skillDictMapper.selectOne(new LambdaQueryWrapper<SkillDict>().eq(SkillDict::getSkillName, skillName));
            if (skillDict == null) {
                skillDict = new SkillDict();
                skillDict.setSkillName(skillName.trim());
                skillDict.setCategory("自定义");
                skillDictMapper.insert(skillDict);
            }
            ResumeSkill entity = new ResumeSkill();
            entity.setResumeId(resumeId);
            entity.setSkillId(skillDict.getId());
            entity.setSkillName(skillDict.getSkillName());
            resumeSkillMapper.insert(entity);
        }
    }

    private void clearEducations(Long resumeId) {
        resumeEducationMapper.delete(new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, resumeId));
    }

    private void clearExperiences(Long resumeId) {
        resumeExperienceMapper.delete(new LambdaQueryWrapper<ResumeExperience>().eq(ResumeExperience::getResumeId, resumeId));
    }

    private void clearProjects(Long resumeId) {
        resumeProjectMapper.delete(new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, resumeId));
    }

    private void clearSkills(Long resumeId) {
        resumeSkillMapper.delete(new LambdaQueryWrapper<ResumeSkill>().eq(ResumeSkill::getResumeId, resumeId));
    }

    private void clearExtraSectionItems(Long resumeId, String sectionCode) {
        resumeExtraSectionItemMapper.delete(new LambdaQueryWrapper<ResumeExtraSectionItem>()
                .eq(ResumeExtraSectionItem::getResumeId, resumeId)
                .eq(ResumeExtraSectionItem::getSectionCode, sectionCode));
    }

    private List<ResumeExtraSectionItem> listExtraSectionItems(Long resumeId, String sectionCode) {
        return resumeExtraSectionItemMapper.selectList(new LambdaQueryWrapper<ResumeExtraSectionItem>()
                .eq(ResumeExtraSectionItem::getResumeId, resumeId)
                .eq(ResumeExtraSectionItem::getSectionCode, sectionCode)
                .orderByAsc(ResumeExtraSectionItem::getSortOrder));
    }

    private List<ResumeDtos.EducationItem> toEducationItems(List<ResumeEducation> entities) {
        return entities.stream().map(entity -> {
            ResumeDtos.EducationItem item = new ResumeDtos.EducationItem();
            item.setSchoolName(entity.getSchoolName());
            item.setMajor(entity.getMajor());
            item.setDegree(entity.getDegree());
            item.setStartDate(entity.getStartDate());
            item.setEndDate(entity.getEndDate());
            item.setCurrent(Boolean.TRUE.equals(entity.getCurrentFlag()));
            item.setDescription(entity.getDescription());
            item.setSortOrder(entity.getSortOrder());
            return item;
        }).toList();
    }

    private List<ResumeDtos.ExperienceItem> toExperienceItems(List<ResumeExperience> entities) {
        return entities.stream().map(entity -> {
            ResumeDtos.ExperienceItem item = new ResumeDtos.ExperienceItem();
            item.setCompanyName(entity.getCompanyName());
            item.setJobTitle(entity.getJobTitle());
            item.setStartDate(entity.getStartDate());
            item.setEndDate(entity.getEndDate());
            item.setCurrent(Boolean.TRUE.equals(entity.getCurrentFlag()));
            item.setDescription(entity.getDescription());
            item.setSortOrder(entity.getSortOrder());
            return item;
        }).toList();
    }

    private List<ResumeDtos.ProjectItem> toProjectItems(List<ResumeProject> entities) {
        return entities.stream().map(entity -> {
            ResumeDtos.ProjectItem item = new ResumeDtos.ProjectItem();
            item.setProjectName(entity.getProjectName());
            item.setRoleName(entity.getRoleName());
            item.setStartDate(entity.getStartDate());
            item.setEndDate(entity.getEndDate());
            item.setCurrent(Boolean.TRUE.equals(entity.getCurrentFlag()));
            item.setDescription(entity.getDescription());
            item.setSortOrder(entity.getSortOrder());
            return item;
        }).toList();
    }

    private List<ResumeDtos.ExtraSectionItem> toExtraSectionItems(List<ResumeExtraSectionItem> entities) {
        return entities.stream().map(entity -> {
            ResumeDtos.ExtraSectionItem item = new ResumeDtos.ExtraSectionItem();
            item.setId(entity.getId());
            item.setTitle(entity.getTitle());
            item.setSubtitle(entity.getSubtitle());
            item.setStartDate(entity.getStartDate());
            item.setEndDate(entity.getEndDate());
            item.setCurrent(Boolean.TRUE.equals(entity.getCurrentFlag()));
            item.setDescription(entity.getDescription());
            item.setSortOrder(entity.getSortOrder());
            return item;
        }).toList();
    }

    private List<ResumeDtos.CustomFieldItem> toCustomFieldItems(List<ResumeExtraSectionItem> entities) {
        return entities.stream().map(entity -> {
            ResumeDtos.CustomFieldItem item = new ResumeDtos.CustomFieldItem();
            item.setKey(entity.getTitle());
            item.setValue(entity.getSubtitle());
            item.setSortOrder(entity.getSortOrder());
            return item;
        }).toList();
    }

    private List<ResumeDtos.ModuleConfigItem> readModuleConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return normalizeModuleConfig(List.of());
        }
        try {
            List<ResumeDtos.ModuleConfigItem> items = objectMapper.readValue(json, new TypeReference<List<ResumeDtos.ModuleConfigItem>>() { });
            return normalizeModuleConfig(items);
        } catch (Exception ex) {
            return normalizeModuleConfig(List.of());
        }
    }

    private List<ResumeDtos.ModuleConfigItem> normalizeModuleConfig(List<ResumeDtos.ModuleConfigItem> incomingItems) {
        Map<String, ResumeDtos.ModuleConfigItem> incomingMap = incomingItems == null
                ? Map.of()
                : incomingItems.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getCode()))
                .collect(Collectors.toMap(ResumeDtos.ModuleConfigItem::getCode, item -> item, (left, right) -> right));
        List<ResumeDtos.ModuleConfigItem> merged = new ArrayList<>();
        for (ResumeDtos.ModuleConfigItem defaults : ResumeModuleConfigSupport.defaultModuleConfig()) {
            ResumeDtos.ModuleConfigItem incoming = incomingMap.get(defaults.getCode());
            ResumeDtos.ModuleConfigItem item = new ResumeDtos.ModuleConfigItem();
            item.setCode(defaults.getCode());
            item.setLabel(defaults.getLabel());
            boolean visible = incoming != null && incoming.getVisible() != null ? incoming.getVisible() : defaults.getVisible();
            if (Objects.equals(defaults.getCode(), ResumeModuleConfigSupport.BASIC_INFO)) {
                visible = true;
            }
            item.setVisible(visible);
            item.setOrder(incoming != null && incoming.getOrder() != null ? incoming.getOrder() : defaults.getOrder());
            merged.add(item);
        }
        merged.sort(Comparator.comparing(item -> Objects.requireNonNullElse(item.getOrder(), Integer.MAX_VALUE)));
        for (int i = 0; i < merged.size(); i++) {
            merged.get(i).setOrder(i);
        }
        return merged;
    }

    private boolean isVisible(List<ResumeDtos.ModuleConfigItem> moduleConfig, String code) {
        return moduleConfig.stream()
                .filter(item -> Objects.equals(item.getCode(), code))
                .findFirst()
                .map(item -> Boolean.TRUE.equals(item.getVisible()))
                .orElse(false);
    }

    private String writeModuleConfig(List<ResumeDtos.ModuleConfigItem> moduleConfig) {
        try {
            return objectMapper.writeValueAsString(moduleConfig);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法序列化简历模块配置", ex);
        }
    }

    private void syncSection(boolean visible, Runnable visibleAction, Runnable hiddenAction) {
        if (visible) {
            visibleAction.run();
            return;
        }
        hiddenAction.run();
    }

    private ResumeDtos.SavedResumeSummaryView toSavedResumeSummaryView(SavedResume savedResume) {
        ResumeDtos.SavedResumeSummaryView view = new ResumeDtos.SavedResumeSummaryView();
        view.setId(savedResume.getId());
        view.setName(savedResume.getName());
        view.setTemplateCode(savedResume.getTemplateCode());
        view.setCompletenessScore(savedResume.getCompletenessScore());
        view.setCompleteFlag(Boolean.TRUE.equals(savedResume.getCompleteFlag()));
        view.setCreatedAt(savedResume.getCreatedAt());
        view.setUpdatedAt(savedResume.getUpdatedAt());
        return view;
    }

    private SavedResume getOwnedSavedResumeOrThrow(Long userId, Long savedResumeId) {
        SavedResume savedResume = savedResumeMapper.selectById(savedResumeId);
        if (savedResume == null || !Objects.equals(savedResume.getUserId(), userId)) {
            throw new BusinessException("已保存简历不存在");
        }
        return savedResume;
    }

    private SavedResume buildSavedResumeEntity(Long userId, String name, String templateCode, Map<String, Object> detail) {
        SavedResume savedResume = new SavedResume();
        savedResume.setUserId(userId);
        populateSavedResumeEntity(savedResume, name, templateCode, detail);
        return savedResume;
    }

    private void populateSavedResumeEntity(SavedResume savedResume, String name, String templateCode, Map<String, Object> detail) {
        savedResume.setName(name);
        savedResume.setTemplateCode(templateCode);
        savedResume.setSnapshotJson(createResumeSnapshotJson(detail));
        List<String> missingItems = extractMissingItems(detail);
        savedResume.setMissingItemsJson(writeJson(missingItems, "无法序列化简历缺失项"));
        savedResume.setCompletenessScore(extractCompletenessScore(detail));
        savedResume.setCompleteFlag(missingItems.isEmpty());
    }

    private List<String> extractMissingItems(Map<String, Object> detail) {
        Object raw = detail.get("missingItems");
        if (raw instanceof List<?> rawList) {
            return rawList.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    private Integer extractCompletenessScore(Map<String, Object> detail) {
        Object raw = detail.get("completenessScore");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private String normalizeSavedResumeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("简历名称不能为空");
        }
        return name.trim();
    }

    private void ensureSavedResumeNameAvailable(Long userId, String name, Long excludeSavedResumeId) {
        LambdaQueryWrapper<SavedResume> wrapper = new LambdaQueryWrapper<SavedResume>()
                .eq(SavedResume::getUserId, userId)
                .eq(SavedResume::getName, name);
        if (excludeSavedResumeId != null) {
            wrapper.ne(SavedResume::getId, excludeSavedResumeId);
        }
        long duplicateCount = savedResumeMapper.selectCount(wrapper);
        if (duplicateCount > 0) {
            throw new BusinessException("简历名称已存在，请更换后重试");
        }
    }

    private List<String> readStringListJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value, String message) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(message, ex);
        }
    }

    private void syncProfile(Long userId, Resume resume) {
        JobseekerProfile profile = jobseekerProfileMapper.selectOne(new LambdaQueryWrapper<JobseekerProfile>()
                .eq(JobseekerProfile::getUserId, userId));
        if (profile == null) {
            return;
        }
        profile.setFullName(resume.getFullName());
        profile.setPhone(resume.getPhone());
        profile.setEmail(resume.getEmail());
        jobseekerProfileMapper.updateById(profile);
    }

    private boolean isEducationItemBlank(ResumeDtos.EducationItem item) {
        return item == null
                || (!StringUtils.hasText(item.getSchoolName())
                && !StringUtils.hasText(item.getMajor())
                && !StringUtils.hasText(item.getDegree())
                && item.getStartDate() == null
                && item.getEndDate() == null
                && !Boolean.TRUE.equals(item.getCurrent())
                && !StringUtils.hasText(item.getDescription()));
    }

    private boolean isExperienceItemBlank(ResumeDtos.ExperienceItem item) {
        return item == null
                || (!StringUtils.hasText(item.getCompanyName())
                && !StringUtils.hasText(item.getJobTitle())
                && item.getStartDate() == null
                && item.getEndDate() == null
                && !Boolean.TRUE.equals(item.getCurrent())
                && !StringUtils.hasText(item.getDescription()));
    }

    private boolean isProjectItemBlank(ResumeDtos.ProjectItem item) {
        return item == null
                || (!StringUtils.hasText(item.getProjectName())
                && !StringUtils.hasText(item.getRoleName())
                && item.getStartDate() == null
                && item.getEndDate() == null
                && !Boolean.TRUE.equals(item.getCurrent())
                && !StringUtils.hasText(item.getDescription()));
    }

    private boolean isExtraSectionItemBlank(ResumeDtos.ExtraSectionItem item) {
        return item == null
                || (!StringUtils.hasText(item.getTitle())
                && !StringUtils.hasText(item.getSubtitle())
                && item.getStartDate() == null
                && item.getEndDate() == null
                && !Boolean.TRUE.equals(item.getCurrent())
                && !StringUtils.hasText(item.getDescription()));
    }

    private void validateEducationItem(ResumeDtos.EducationItem item, int index) {
        if (!StringUtils.hasText(item.getSchoolName())) {
            throw new BusinessException("教育背景第" + (index + 1) + "条请填写学校名称");
        }
        validateDateRange("教育背景", index, item.getStartDate(), item.getEndDate(), item.getCurrent());
    }

    private void validateExperienceItem(ResumeDtos.ExperienceItem item, int index) {
        if (!StringUtils.hasText(item.getCompanyName())) {
            throw new BusinessException("工作经历第" + (index + 1) + "条请填写公司名称");
        }
        if (!StringUtils.hasText(item.getJobTitle())) {
            throw new BusinessException("工作经历第" + (index + 1) + "条请填写职位");
        }
        validateDateRange("工作经历", index, item.getStartDate(), item.getEndDate(), item.getCurrent());
    }

    private void validateProjectItem(ResumeDtos.ProjectItem item, int index) {
        if (!StringUtils.hasText(item.getProjectName())) {
            throw new BusinessException("项目经历第" + (index + 1) + "条请填写项目名称");
        }
        validateDateRange("项目经历", index, item.getStartDate(), item.getEndDate(), item.getCurrent());
    }

    private void validateExtraSectionItem(ResumeDtos.ExtraSectionItem item, int index, String sectionCode) {
        if (!StringUtils.hasText(item.getTitle())) {
            throw new BusinessException(sectionLabel(sectionCode) + "第" + (index + 1) + "条请填写标题");
        }
        if (supportsCurrentFlag(sectionCode)) {
            validateDateRange(sectionLabel(sectionCode), index, item.getStartDate(), item.getEndDate(), item.getCurrent());
        } else if (item.getStartDate() == null && item.getEndDate() != null) {
            throw new BusinessException(sectionLabel(sectionCode) + "第" + (index + 1) + "条请先填写开始时间");
        } else if (item.getStartDate() != null && item.getEndDate() != null && item.getEndDate().isBefore(item.getStartDate())) {
            throw new BusinessException(sectionLabel(sectionCode) + "第" + (index + 1) + "条结束时间不能早于开始时间");
        }
    }

    private void validateDateRange(String sectionLabel, int index, LocalDate startDate, LocalDate endDate, Boolean current) {
        if (startDate == null && (endDate != null || Boolean.TRUE.equals(current))) {
            throw new BusinessException(sectionLabel + "第" + (index + 1) + "条请先填写开始时间");
        }
        if (startDate != null && endDate == null && !Boolean.TRUE.equals(current)) {
            throw new BusinessException(sectionLabel + "第" + (index + 1) + "条请选择结束时间或勾选至今");
        }
        if (!Boolean.TRUE.equals(current) && startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(sectionLabel + "第" + (index + 1) + "条结束时间不能早于开始时间");
        }
    }

    private String sectionLabel(String sectionCode) {
        return switch (sectionCode) {
            case SECTION_INTERNSHIP -> "实习经历";
            case SECTION_CAMPUS -> "校园经历";
            case SECTION_HONOR -> "荣誉证书";
            case SECTION_HOBBY -> "兴趣爱好";
            case SECTION_CUSTOM -> "自定义信息";
            default -> "模块";
        };
    }

    private boolean supportsCurrentFlag(String sectionCode) {
        return SECTION_INTERNSHIP.equals(sectionCode) || SECTION_CAMPUS.equals(sectionCode);
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            return null;
        }
        return Period.between(birthDate, today).getYears();
    }
}
