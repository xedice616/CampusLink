package az.campuslink;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class CareerModels {
 private CareerModels() {}
 public enum Category {TECHNICAL_SKILLS, PROJECT_RELEVANCE, RELEVANT_EXPERIENCE, EDUCATION_QUALIFICATIONS, EVIDENCE_QUALITY, READABILITY_STRUCTURE}
 public enum Alignment {MATCHED, PARTIAL, MISSING}
 public record Evidence(@NotBlank @Size(max=200) String label, @NotBlank @Size(max=600) String excerpt) {}
 public record Requirement(@NotNull Category category, @NotBlank @Size(max=300) String requirement,
   @NotBlank @Size(max=600) String jobEvidence, @NotNull Alignment alignment,
   @NotNull @Size(max=5) List<@NotBlank @Size(max=600) String> cvEvidence,
   @NotBlank @Size(max=800) String explanation) {}
 public record Quality(@NotNull Category category, @Min(0) @Max(100) int score,
   @NotBlank @Size(max=800) String explanation, @NotNull @Size(min=1,max=5) List<@NotBlank @Size(max=600) String> cvEvidence) {}
 public record Bullet(@NotBlank @Size(max=600) String original, @NotBlank @Size(max=1000) String improved) {}
 public record Roadmap(@Min(30) @Max(90) int day, @NotNull @Size(min=1,max=6) List<@NotBlank @Size(max=400) String> actions) {}
 public record Draft(
   @NotBlank @Size(max=1200) String summary,
   @NotNull @Size(max=35) List<@Valid Evidence> skills,
   @NotNull @Size(max=12) List<@Valid Evidence> education,
   @NotNull @Size(max=12) List<@Valid Evidence> projects,
   @NotNull @Size(max=12) List<@Valid Evidence> experience,
   @NotNull @Size(max=30) List<@Valid Requirement> requirements,
   @NotNull @Size(min=2,max=2) List<@Valid Quality> quality,
   @NotNull @Size(max=12) List<@NotBlank @Size(max=400) String> missingInformation,
   @NotNull @Size(max=8) List<@NotBlank @Size(max=400) String> strengths,
   @NotNull @Size(max=8) List<@NotBlank @Size(max=400) String> priorities,
   @NotNull @Size(max=10) List<@NotBlank @Size(max=200) String> skillsToDevelop,
   @NotNull @Size(max=10) List<@NotBlank @Size(max=400) String> learningTopics,
   @NotNull @Size(max=6) List<@NotBlank @Size(max=600) String> portfolioIdeas,
   @NotNull @Size(max=6) List<@Valid Bullet> bulletImprovements,
   @NotNull @Size(max=10) List<@NotBlank @Size(max=400) String> interviewTopics,
   @NotNull @Size(min=3,max=3) List<@Valid Roadmap> roadmap,
   @DecimalMin("0.0") @DecimalMax("1.0") double confidence,
   @NotNull @Size(min=1,max=8) List<@NotBlank @Size(max=400) String> limitations) {}
 public record CategoryScore(Category category, Integer score, int weight, String explanation) {}
 public record MatchResult(Integer overallScore, String scoreKind, List<CategoryScore> categories,
   List<String> matchedRequirements, List<String> missingRequirements) {}
 public record Result(String id, String type, String provider, String model, String language, String createdAt,
   String expiresAt, boolean saved, String role, MatchResult matching, Draft analysis, List<String> limitations) {}
 public record Access(boolean freeAvailable, boolean pro, boolean busy, boolean providerConfigured,
   String provider, String model, int retentionDays, long maxFileBytes) {}
}
