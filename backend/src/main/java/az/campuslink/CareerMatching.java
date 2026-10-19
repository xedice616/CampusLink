package az.campuslink;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.*;
import static az.campuslink.CareerModels.*;

@Component
public class CareerMatching {
 private static final Map<Category,Integer> WEIGHTS=Map.of(Category.TECHNICAL_SKILLS,30,Category.PROJECT_RELEVANCE,20,Category.RELEVANT_EXPERIENCE,20,Category.EDUCATION_QUALIFICATIONS,10,Category.EVIDENCE_QUALITY,10,Category.READABILITY_STRUCTURE,10);
 private void ensure(boolean condition){if(!condition)throw new ApiException(HttpStatus.BAD_GATEWAY,"AI evidence validation failed. No quota was consumed.");}
 private boolean quote(String source,String excerpt){return excerpt!=null&&!excerpt.isBlank()&&source.contains(excerpt);}
 public MatchResult calculate(Draft d,String cv,String job){
  for(var list:List.of(d.skills(),d.projects(),d.education(),d.experience()))for(var fact:list)ensure(quote(cv,fact.excerpt()));
  for(var bullet:d.bulletImprovements())ensure(quote(cv,bullet.original()));
  ensure(d.roadmap().stream().map(Roadmap::day).sorted().toList().equals(List.of(30,60,90)));
  ensure(d.quality().stream().map(Quality::category).collect(java.util.stream.Collectors.toSet()).equals(Set.of(Category.EVIDENCE_QUALITY,Category.READABILITY_STRUCTURE)));
  boolean targeted=!job.isBlank();
  if(!targeted)ensure(d.requirements().isEmpty());
  Set<String> seen=new HashSet<>();
  for(var r:d.requirements()){
   ensure(!java.util.regex.Pattern.compile("(?iu)\\b(gender|religion|ethnicity|race|nationality|marital|disability|pregnancy|cinsiyet|milliyet|religiya|geschlecht|nationalität|familienstand|behinderung|национальность|религия|инвалидность|возраст|cins|milliyyət|din|əlillik)\\b").matcher(r.requirement()+" "+r.jobEvidence()).find());
   ensure(r.category()!=Category.EVIDENCE_QUALITY&&r.category()!=Category.READABILITY_STRUCTURE);
   ensure(quote(job,r.jobEvidence())&&seen.add(r.category()+":"+r.requirement().toLowerCase(Locale.ROOT)));
   if(r.alignment()==Alignment.MISSING)ensure(r.cvEvidence().isEmpty());else ensure(!r.cvEvidence().isEmpty());
   for(String q:r.cvEvidence())ensure(quote(cv,q));
  }
  // Nonempty JD with no extracted requirements is explicitly insufficient, not a perfect match.
  List<CategoryScore> categories=new ArrayList<>();double sum=0;int total=0;
  for(Category c:Category.values()){
   int weight=WEIGHTS.get(c);Integer score=null;String explanation;
   if(c==Category.EVIDENCE_QUALITY||c==Category.READABILITY_STRUCTURE){
    Quality quality=d.quality().stream().filter(q->q.category()==c).findFirst().orElseThrow();
    for(String q:quality.cvEvidence())ensure(quote(cv,q));
    score=quality.score();explanation=quality.explanation();
   }else{
    var requirements=d.requirements().stream().filter(r->r.category()==c).toList();
    if(requirements.isEmpty())explanation="Not assessed: no explicit job requirements for this category.";
    else {double points=requirements.stream().mapToDouble(r->switch(r.alignment()){case MATCHED->100;case PARTIAL->50;case MISSING->0;}).sum();
     score=(int)Math.round(points/requirements.size());explanation="Mean requirement alignment: matched 100, partial 50, missing 0. See quoted evidence.";}
   }
   if(score!=null){sum+=score*weight;total+=weight;}
   categories.add(new CategoryScore(c,score,weight,explanation));
  }
  Integer overall=targeted&&d.requirements().isEmpty()?null:(int)Math.round(sum/total);
  return new MatchResult(overall,targeted?"job-compatibility-heuristic":"general-cv-quality",categories,
    d.requirements().stream().filter(r->r.alignment()==Alignment.MATCHED).map(Requirement::requirement).toList(),
    d.requirements().stream().filter(r->r.alignment()!=Alignment.MATCHED).map(Requirement::requirement).toList());
 }
}
