package az.campuslink;
import static az.campuslink.CareerModels.*;
public interface CareerAi {
 boolean configured();
 String provider();
 String model();
 Draft analyze(String cv,String job,String role,String language);
}
