package az.campuslink;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Uses the same hashed session tokens and expiry as the existing API. */
@Component
public class CareerAuth {
 private final JdbcTemplate db;
 public CareerAuth(JdbcTemplate db){this.db=db;}
 public long userId(HttpServletRequest req){
  String header=req.getHeader("Authorization");
  if(header==null||!header.matches("Bearer [a-f0-9]{64}"))throw new ApiException(HttpStatus.UNAUTHORIZED,"Please sign in");
  try {
   String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(header.substring(7).getBytes(StandardCharsets.UTF_8)));
   var ids=db.queryForList("SELECT s.user_id FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>?",Long.class,hash,System.currentTimeMillis());
   if(ids.isEmpty())throw new ApiException(HttpStatus.UNAUTHORIZED,"Session expired. Sign in again");
   return ids.getFirst();
  }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
}
