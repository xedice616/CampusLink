package az.campuslink;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/career")
public class CareerController {
 private final CareerAuth auth;private final CareerService service;
 public CareerController(CareerAuth auth,CareerService service){this.auth=auth;this.service=service;}
 @GetMapping("/access") public Object access(HttpServletRequest req){return service.access(auth.userId(req));}
 @PostMapping(value="/preview",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Object preview(HttpServletRequest req,@RequestParam MultipartFile file){return service.preview(auth.userId(req),file);}
 @PostMapping(value="/analyze",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Object analyze(HttpServletRequest req,@RequestParam MultipartFile file,
  @RequestParam(defaultValue="") String role,@RequestParam(defaultValue="") String jobDescription,
  @RequestParam(defaultValue="en") String language,@RequestParam(defaultValue="false") boolean consent,
  @RequestParam(defaultValue="false") boolean saveHistory){return service.analyze(auth.userId(req),file,role,jobDescription,language,consent,saveHistory);}
 /** Preserve the existing route, reject legacy skill-only JSON explicitly rather than fabricate AI results. */
 @PostMapping(value="/analyze",consumes=MediaType.APPLICATION_JSON_VALUE)
 public Object legacy(HttpServletRequest req){auth.userId(req);throw new ApiException(HttpStatus.BAD_REQUEST,"Career analysis now requires multipart/form-data with a PDF/DOCX file and explicit consent. Legacy skill-only JSON is unsupported.");}
 @GetMapping("/analyses") public Object history(HttpServletRequest req){return service.history(auth.userId(req));}
 @GetMapping("/analyses/{id}") public Object result(HttpServletRequest req,@PathVariable String id){return service.result(auth.userId(req),id);}
 @DeleteMapping("/analyses/{id}") public Object delete(HttpServletRequest req,@PathVariable String id){service.delete(auth.userId(req),id);return Map.of("deleted",true);}
 @DeleteMapping("/analyses") public Object deleteAll(HttpServletRequest req){service.deleteAll(auth.userId(req));return Map.of("deleted",true,"quotaReset",false);}
}
