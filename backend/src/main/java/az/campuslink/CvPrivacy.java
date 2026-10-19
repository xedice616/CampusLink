package az.campuslink;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class CvPrivacy {
 private final JdbcTemplate db;
 public CvPrivacy(JdbcTemplate db){this.db=db;}
 public String minimize(long user,String text){
  String cleaned=text.replaceAll("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}","[EMAIL]")
    .replaceAll("(?<!\\w)\\+?\\d[\\d ()-]{7,}\\d(?!\\w)","[PHONE]")
    .replaceAll("(?i)https?://\\S+|www\\.\\S+","[URL]")
    .replaceAll("(?im)^\\s*(?:name|full name|address|date of birth|dob|gender|sex|nationality|religion|marital status|disability|age|ad soyad|ünvan|doğum tarixi|имя|адрес|дата рождения|geschlecht|geburtsdatum|adresse|cinsiyet|doğum tarihi)\\s*[:：].*$","[PERSONAL DETAILS REMOVED]");
  var names=db.queryForList("SELECT full_name FROM users WHERE id=?",String.class,user);
  if(!names.isEmpty()&&!names.getFirst().isBlank())cleaned=cleaned.replaceAll("(?iu)"+Pattern.quote(names.getFirst()),"[NAME]");
  return cleaned;
 }
}
