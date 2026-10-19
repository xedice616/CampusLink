package az.campuslink;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CareerPersistenceTest {
 @TempDir Path directory;
 @Test void quotaEntitlementAndResultsSurviveFileDbReopen(){
  String url="jdbc:h2:file:"+directory.resolve("persistence").toAbsolutePath().toString().replace('\\','/')+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
  var first=new DriverManagerDataSource(url,"sa","");new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(first);var db=new JdbcTemplate(first);
  db.update("INSERT INTO users(id,full_name,email,password_hash) VALUES(100,'Fixture','fixture@example.test','not-a-login-hash')");
  db.update("INSERT INTO career_access(user_id,free_used,pro_until) VALUES(100,TRUE,9999999999999)");
  db.update("INSERT INTO career_results(id,user_id,created_at,expires_at,result_json) VALUES('fixture',100,1,9999999999999,'{}')");
  db.execute("SHUTDOWN");
  var reopened=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
  assertTrue(reopened.queryForObject("SELECT free_used FROM career_access WHERE user_id=100",Boolean.class));
  assertEquals(9999999999999L,reopened.queryForObject("SELECT pro_until FROM career_access WHERE user_id=100",Long.class));
  assertEquals(1,reopened.queryForObject("SELECT COUNT(*) FROM career_results WHERE user_id=100",Integer.class));reopened.execute("SHUTDOWN");
 }
}
