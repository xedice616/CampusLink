package az.campuslink;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CareerMatchingTest {
 final CareerMatching matching=new CareerMatching();
 @Test void calculatesExplainableWeightedScore(){
  var result=matching.calculate(CareerFixtures.draft(true),CareerFixtures.TEXT,CareerFixtures.JD);
  // Technical (100+50)/2=75 at weight30, quality70/80 at weight10 each:75.
  assertEquals(75,result.overallScore());assertEquals(1,result.matchedRequirements().size());assertEquals(1,result.missingRequirements().size());
 }
 @Test void generalModeDoesNotInventRequirements(){
  var result=matching.calculate(CareerFixtures.draft(false),CareerFixtures.TEXT,"");
  assertEquals("general-cv-quality",result.scoreKind());assertTrue(result.matchedRequirements().isEmpty());assertEquals(75,result.overallScore());
  assertThrows(ApiException.class,()->matching.calculate(CareerFixtures.draft(true),CareerFixtures.TEXT,""));
 }
 @Test void rejectsInventedEvidenceAndJobRequirements(){
  assertThrows(ApiException.class,()->matching.calculate(CareerFixtures.draft(true),"Invented unrelated CV",CareerFixtures.JD));
  assertThrows(ApiException.class,()->matching.calculate(CareerFixtures.draft(true),CareerFixtures.TEXT,"Unrelated job description"));
 }
}
