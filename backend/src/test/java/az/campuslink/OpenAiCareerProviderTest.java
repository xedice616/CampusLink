package az.campuslink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiCareerProviderTest {
 final ObjectMapper json=new ObjectMapper();
 HttpServer server;jakarta.validation.ValidatorFactory validation;
 final AtomicReference<String> received=new AtomicReference<>();
 int code=200;String body;long delay=0;
 @BeforeEach void start()throws Exception{
  validation=Validation.buildDefaultValidatorFactory();body=envelope(json.writeValueAsString(CareerFixtures.draft(false)));
  server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/responses",exchange->{try{
   received.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
   if(delay>0)Thread.sleep(delay);byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
   exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);
  }catch(InterruptedException e){Thread.currentThread().interrupt();}finally{exchange.close();}});server.start();
 }
 @AfterEach void stop(){server.stop(0);validation.close();}
 String envelope(String text)throws Exception{return json.writeValueAsString(Map.of("status","completed","output",List.of(Map.of("content",List.of(Map.of("type","output_text","text",text))))));}
 OpenAiCareerProvider provider(Duration timeout){return new OpenAiCareerProvider("openai","test-model","test-placeholder",timeout,json,validation.getValidator(),HttpClient.newHttpClient(),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/responses"));}
 @Test void structuredRequestHasNoStorageToolsOrRawFileAndUsesUntrustedDataPrompt()throws Exception{
  var result=provider(Duration.ofSeconds(10)).analyze(CareerFixtures.TEXT,"","Java","az");assertEquals("Synthetic test analysis",result.summary());
  var request=json.readTree(received.get());assertFalse(request.path("store").asBoolean());assertFalse(request.has("tools"));assertTrue(request.path("instructions").asText().contains("UNTRUSTED DATA"));
  assertEquals("json_schema",request.at("/text/format/type").asText());assertTrue(request.at("/text/format/strict").asBoolean());assertFalse(request.at("/text/format/schema/additionalProperties").asBoolean());
  assertEquals("az",json.readTree(request.path("input").asText()).path("responseLanguage").asText());assertFalse(received.get().contains("test-placeholder"));
 }
 @Test void rejectsMalformedJsonSchemaViolationsUnknownFieldsAndTruncation()throws Exception{
  var provider=provider(Duration.ofSeconds(10));
  for(String invalid:List.of("not json","{}",json.writeValueAsString(CareerFixtures.draft(false)).replace("\"confidence\":0.7","\"confidence\":2.0"),json.writeValueAsString(CareerFixtures.draft(false)).replace("\"summary\":","\"unknown\":true,\"summary\":"))){body=envelope(invalid);assertThrows(ApiException.class,()->provider.analyze(CareerFixtures.TEXT,"","Java","en"));}
  body="{\"status\":\"incomplete\",\"output\":[]}";assertThrows(ApiException.class,()->provider.analyze(CareerFixtures.TEXT,"","Java","en"));
  body="{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"refusal\"}]}]}";assertThrows(ApiException.class,()->provider.analyze(CareerFixtures.TEXT,"","Java","en"));
 }
 @Test void providerErrorsNeverProduceFallbackResults(){
  var provider=provider(Duration.ofSeconds(10));for(int status:new int[]{401,429,500}){code=status;assertThrows(ApiException.class,()->provider.analyze(CareerFixtures.TEXT,"","Java","en"));}
 }
 @Test void enforcesTimeout(){delay=6500;var error=assertThrows(ApiException.class,()->provider(Duration.ofSeconds(5)).analyze(CareerFixtures.TEXT,"","Java","en"));assertEquals(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT,error.status);}
 @Test void blankCredentialsDisableProvider(){var p=new OpenAiCareerProvider("openai","model","",Duration.ofSeconds(10),json,validation.getValidator());assertFalse(p.configured());assertThrows(ApiException.class,()->p.analyze("cv","","","en"));}
}
