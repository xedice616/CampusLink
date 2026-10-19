package az.campuslink;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

/** Authenticate before Spring resolves uploaded multipart documents. */
@Component
@Order(10)
public class CareerSecurityFilter extends OncePerRequestFilter {
 private final CareerAuth auth;private final ObjectMapper json;
 public CareerSecurityFilter(CareerAuth auth,ObjectMapper json){this.auth=auth;this.json=json;}
 @Override protected boolean shouldNotFilter(HttpServletRequest req){return !req.getRequestURI().startsWith("/api/v1/career/");}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  res.setHeader("Cache-Control","no-store");res.setHeader("X-Content-Type-Options","nosniff");
  try{auth.userId(req);}catch(ApiException e){res.setStatus(e.status.value());res.setContentType("application/json");json.writeValue(res.getOutputStream(),Map.of("message",e.getMessage()));return;}
  chain.doFilter(req,res);
 }
}
