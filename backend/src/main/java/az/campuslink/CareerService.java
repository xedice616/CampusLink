package az.campuslink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import static az.campuslink.CareerModels.*;

@Service
public class CareerService {
 private final JdbcTemplate db;private final TransactionTemplate tx;private final ObjectMapper json;
 private final CvExtractor extractor;private final CvPrivacy privacy;private final CareerAi ai;private final CareerMatching matching;
 private final int retentionDays;private final Semaphore capacity=new Semaphore(4);
 public CareerService(JdbcTemplate db,TransactionTemplate tx,ObjectMapper json,CvExtractor extractor,CvPrivacy privacy,CareerAi ai,CareerMatching matching,
   @Value("${career.retention-days:30}") int days){
  this.db=db;this.tx=tx;this.json=json;this.extractor=extractor;this.privacy=privacy;this.ai=ai;this.matching=matching;
  if(days<1||days>90)throw new IllegalArgumentException("CV_RETENTION_DAYS must be 1..90");retentionDays=days;
 }
 private void ensureAccount(long user){
  if(db.queryForObject("SELECT COUNT(*) FROM career_access WHERE user_id=?",Integer.class,user)==0)
   try{db.update("INSERT INTO career_access(user_id) VALUES(?)",user);}catch(DuplicateKeyException ignored){}
 }
 public Access access(long user){
  ensureAccount(user);var row=db.queryForMap("SELECT * FROM career_access WHERE user_id=?",user);long now=System.currentTimeMillis();
  return new Access(!Boolean.TRUE.equals(row.get("free_used")),((Number)row.get("pro_until")).longValue()>now,
    row.get("active_request")!=null&&((Number)row.get("active_until")).longValue()>now,ai.configured(),ai.provider(),ai.model(),retentionDays,extractor.maxBytes());
 }
 public Map<String,Object> preview(long user,MultipartFile file){
  String redacted=privacy.minimize(user,extractor.extract(file));
  return Map.of("redactedText",redacted,"characters",redacted.length(),"disclosure","Contact details and known account name are removed automatically. Other identifying information may remain; review your resume. Only extracted text is transmitted after consent, never the original file.");
 }
 public Result analyze(long user,MultipartFile file,String role,String job,String language,boolean consent,boolean save){
  if(!consent)throw new ApiException(HttpStatus.BAD_REQUEST,"Consent to external AI processing is required");
  if(!Set.of("az","en","ru","tr","de").contains(language))throw new ApiException(HttpStatus.BAD_REQUEST,"Choose response language: az, en, ru, tr or de");
  if(role.length()>200||job.length()>12000)throw new ApiException(HttpStatus.BAD_REQUEST,"Target role limit is 200 characters; job description limit is 12,000");
  if(!ai.configured())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"AI is not configured. No analysis has been charged.");
  if(!capacity.tryAcquire())throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,"Analysis capacity is busy. Try later.");
  String lease=null;
  try{
   // Invalid files never reserve quota. Inputs are not persisted.
   String cv=privacy.minimize(user,extractor.extract(file));
   String safeJob=privacy.minimize(user,job.strip()),safeRole=privacy.minimize(user,role.strip());
   ensureAccount(user);lease=UUID.randomUUID().toString();long now=System.currentTimeMillis();
   int claimed=db.update("UPDATE career_access SET active_request=?,active_until=?,active_canceled=FALSE WHERE user_id=? AND (free_used=FALSE OR pro_until>?) AND (active_request IS NULL OR active_until<?)",lease,now+Duration.ofMinutes(5).toMillis(),user,now,now);
   if(claimed==0){var state=access(user);throw new ApiException(state.busy()?HttpStatus.CONFLICT:HttpStatus.PAYMENT_REQUIRED,state.busy()?"An analysis is already running for this account":"Your free analysis has been used. A valid PRO entitlement is required.");}
   Draft draft=ai.analyze(cv,safeJob,safeRole,language);
   MatchResult match=matching.calculate(draft,cv,safeJob);
   String id=UUID.randomUUID().toString();long created=System.currentTimeMillis(),expires=created+Duration.ofDays(retentionDays).toMillis();
   Result result=new Result(id,"ai-cv-analysis",ai.provider(),ai.model(),language,Instant.ofEpochMilli(created).toString(),save?Instant.ofEpochMilli(expires).toString():null,
     save,safeRole,match,draft,List.of("Scores are heuristic guidance, never guaranteed ATS scores or hiring probabilities.",
       "Only text was evaluated; scanned text/OCR and visual PDF layout are unsupported.","AI can be wrong. Verbatim quotes verify text presence, not truth or semantic correctness.",
       "Automated redaction is incomplete; avoid including sensitive personal information."));
   final String activeLease=lease;
   tx.executeWithoutResult(status->{
    if(db.update("UPDATE career_access SET free_used=TRUE,active_request=NULL,active_until=0 WHERE user_id=? AND active_request=? AND active_until>? AND active_canceled=FALSE",user,activeLease,System.currentTimeMillis())!=1)
     throw new ApiException(HttpStatus.CONFLICT,"Analysis was canceled or expired; result was not saved.");
    if(save){try{db.update("INSERT INTO career_results(id,user_id,created_at,expires_at,result_json) VALUES(?,?,?,?,?)",id,user,created,expires,json.writeValueAsString(result));}
     catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Result serialization failed");}}
   });
   return result;
  }finally{
   try{if(lease!=null)db.update("UPDATE career_access SET active_request=NULL,active_until=0 WHERE user_id=? AND active_request=?",user,lease);}
   finally{capacity.release();}
  }
 }
 public List<Map<String,Object>> history(long user){
  purge();return db.query("SELECT id,created_at,expires_at FROM career_results WHERE user_id=? ORDER BY created_at DESC",(row,index)->Map.<String,Object>of("id",row.getString("id"),"createdAt",row.getLong("created_at"),"expiresAt",row.getLong("expires_at")),user);
 }
 public Result result(long user,String id){
  purge();var rows=db.queryForList("SELECT result_json FROM career_results WHERE id=? AND user_id=?",String.class,id,user);
  if(rows.isEmpty())throw new ApiException(HttpStatus.NOT_FOUND,"Analysis not found or expired");
  try{return json.readValue(rows.getFirst(),Result.class);}catch(Exception e){throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"Stored analysis could not be read");}
 }
 public void delete(long user,String id){if(db.update("DELETE FROM career_results WHERE id=? AND user_id=?",id,user)==0)throw new ApiException(HttpStatus.NOT_FOUND,"Analysis not found");}
 public void deleteAll(long user){
  tx.executeWithoutResult(s->{ensureAccount(user);db.update("UPDATE career_access SET active_canceled=TRUE WHERE user_id=?",user);db.update("DELETE FROM career_results WHERE user_id=?",user);});
 }
 @Scheduled(fixedDelay=3600000)
 public void purge(){db.update("DELETE FROM career_results WHERE expires_at<=?",System.currentTimeMillis());}
}
