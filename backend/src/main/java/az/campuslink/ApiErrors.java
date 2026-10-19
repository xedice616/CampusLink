package az.campuslink;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiErrors {
@ExceptionHandler(ApiException.class) ResponseEntity<Map<String,String>> handle(ApiException e){return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage()));}
@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class) ResponseEntity<Map<String,String>> bad(){return ResponseEntity.badRequest().body(Map.of("message","Invalid JSON"));}
@ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) ResponseEntity<Map<String,String>> tooLarge(){return ResponseEntity.status(413).body(Map.of("message","Upload exceeds the configured size limit"));}
@ExceptionHandler({org.springframework.web.multipart.MultipartException.class,org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.multipart.support.MissingServletRequestPartException.class}) ResponseEntity<Map<String,String>> upload(){return ResponseEntity.badRequest().body(Map.of("message","Invalid upload. Send multipart/form-data with a file field."));}
}
