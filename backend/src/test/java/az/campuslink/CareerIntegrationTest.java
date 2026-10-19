package az.campuslink;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.concurrent.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:career-tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","career.ai.model=test-model","career.ai.api-key=test-only-not-a-real-key"})
@AutoConfigureMockMvc
class CareerIntegrationTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired CareerService service;@Autowired CvPrivacy privacy;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 @MockBean CareerAi ai;
 @BeforeEach void configure(){when(ai.configured()).thenReturn(true);when(ai.provider()).thenReturn("test-provider");when(ai.model()).thenReturn("fixture-model");when(ai.analyze(anyString(),anyString(),anyString(),anyString())).thenAnswer(i->CareerFixtures.draft(!i.getArgument(1,String.class).isBlank()));}
 String token()throws Exception{
  var response=mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(json.writeValueAsString(Map.of("fullName","Test Student","email",UUID.randomUUID()+"@example.test","password","test-password-123")))).andExpect(status().isOk()).andReturn();
  return json.readTree(response.getResponse().getContentAsString()).path("token").asText();
 }
 long user(String token)throws Exception{
  var response=mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andReturn();return json.readTree(response.getResponse().getContentAsString()).path("id").asLong();
 }
 @Test void authOwnershipDeletionAndQuota()throws Exception{
  mvc.perform(get("/api/v1/career/access")).andExpect(status().isUnauthorized());
  mvc.perform(multipart("/api/v1/career/analyze").file(CareerFixtures.pdf())).andExpect(status().isUnauthorized());
  String owner=token(),other=token();
  var response=mvc.perform(multipart("/api/v1/career/analyze").file(CareerFixtures.pdf()).param("consent","true").param("saveHistory","true").param("jobDescription",CareerFixtures.JD).header("Authorization","Bearer "+owner)).andExpect(status().isOk()).andExpect(jsonPath("matching.overallScore").value(75)).andReturn();
  String id=json.readTree(response.getResponse().getContentAsString()).path("id").asText();
  mvc.perform(get("/api/v1/career/analyses/"+id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/v1/career/analyses/"+id).header("Authorization","Bearer "+other)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/career/analyses/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isOk());
  mvc.perform(delete("/api/v1/career/analyses").header("Authorization","Bearer "+owner)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/career/analyses/"+id).header("Authorization","Bearer "+owner)).andExpect(status().isNotFound());
  mvc.perform(multipart("/api/v1/career/analyze").file(CareerFixtures.pdf()).param("consent","true").header("Authorization","Bearer "+owner)).andExpect(status().isPaymentRequired());
 }
 @Test void failureAndConsentDoNotConsumeFreeAccess()throws Exception{
  String token=token();
  mvc.perform(multipart("/api/v1/career/analyze").file(CareerFixtures.pdf()).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
  when(ai.analyze(anyString(),anyString(),anyString(),anyString())).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY,"Provider failed"));
  mvc.perform(multipart("/api/v1/career/analyze").file(CareerFixtures.pdf()).param("consent","true").header("Authorization","Bearer "+token)).andExpect(status().isBadGateway());
  mvc.perform(get("/api/v1/career/access").header("Authorization","Bearer "+token)).andExpect(jsonPath("freeAvailable").value(true)).andExpect(jsonPath("busy").value(false));
 }
 @Test void legacyRouteExplicitlyRejectsSkillOnlyPayloadAndOtherEndpointsWork()throws Exception{
  String token=token();mvc.perform(post("/api/v1/career/analyze").header("Authorization","Bearer "+token).contentType("application/json").content("{\"role\":\"Java\",\"skills\":\"java\"}")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/health")).andExpect(status().isOk());mvc.perform(get("/api/v1/courses")).andExpect(status().isOk());mvc.perform(get("/api/v1/teams")).andExpect(status().isOk());
 }
 @Test void concurrentRequestsHaveOneWinnerAndDbEntitlementAllowsPro()throws Exception{
  long user=user(token());var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
  when(ai.analyze(anyString(),anyString(),anyString(),anyString())).thenAnswer(i->{entered.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return CareerFixtures.draft(false);});
  try(var pool=Executors.newFixedThreadPool(2)){
   var first=pool.submit(()->service.analyze(user,CareerFixtures.pdf(),"Java","","en",true,false));
   assertTrue(entered.await(10,TimeUnit.SECONDS));
   try{var e=assertThrows(ApiException.class,()->service.analyze(user,CareerFixtures.pdf(),"Java","","en",true,false));assertEquals(HttpStatus.CONFLICT,e.status);}finally{release.countDown();}
   assertNotNull(first.get(10,TimeUnit.SECONDS));assertFalse(service.access(user).freeAvailable());
  }
  db.update("UPDATE career_access SET pro_until=? WHERE user_id=?",System.currentTimeMillis()+60000,user);
  assertTrue(service.access(user).pro());assertNotNull(service.analyze(user,CareerFixtures.pdf(),"Java","","de",true,false));
 }
 @Test void privacyPreviewAndEphemeralDefault()throws Exception{
  String token=token();long user=user(token());
  String clean=privacy.minimize(user,"Test Student email@student.test +994 50 123 45 67\nDate of birth: 2000-01-01\nJava SQL");
  assertFalse(clean.contains("Test Student"));assertFalse(clean.contains("email@student.test"));assertFalse(clean.contains("123 45 67"));assertFalse(clean.contains("2000-01-01"));assertTrue(clean.contains("Java SQL"));
  mvc.perform(multipart("/api/v1/career/preview").file(CareerFixtures.pdf()).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("redactedText").exists());
  var result=service.analyze(user,CareerFixtures.pdf(),"Java","","az",true,false);
  assertFalse(result.saved());assertTrue(service.history(user).isEmpty());assertFalse(service.access(user).freeAvailable());
 }
 @Test void retentionPurgePreservesQuota()throws Exception{
  long user=user(token());var result=service.analyze(user,CareerFixtures.pdf(),"Java","","tr",true,true);
  db.update("UPDATE career_results SET expires_at=0 WHERE id=?",result.id());service.purge();
  assertThrows(ApiException.class,()->service.result(user,result.id()));assertFalse(service.access(user).freeAvailable());
 }
 @Test void deletingDuringRequestPreventsResultPersistence()throws Exception{
  long user=user(token());when(ai.analyze(anyString(),anyString(),anyString(),anyString())).thenAnswer(i->{service.deleteAll(user);return CareerFixtures.draft(false);});
  assertThrows(ApiException.class,()->service.analyze(user,CareerFixtures.pdf(),"Java","","en",true,true));assertTrue(service.history(user).isEmpty());assertTrue(service.access(user).freeAvailable());
 }
}
