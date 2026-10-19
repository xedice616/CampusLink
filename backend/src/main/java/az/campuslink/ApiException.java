package az.campuslink;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {final HttpStatus status;ApiException(HttpStatus s,String message){super(message);status=s;}}
