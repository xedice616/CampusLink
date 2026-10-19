package az.campuslink;

import java.lang.reflect.*;
import java.util.*;

/** Closed JSON schema generated from the same record types used for validation. */
final class StructuredSchema {
 private StructuredSchema(){}
 static Map<String,Object> schema(Type type){
  if(type instanceof ParameterizedType p&&p.getRawType()==List.class)return Map.of("type","array","items",schema(p.getActualTypeArguments()[0]));
  Class<?> c=(Class<?>)type;
  if(c==String.class)return Map.of("type","string");
  if(c==int.class)return Map.of("type","integer");
  if(c==double.class)return Map.of("type","number");
  if(c.isEnum())return Map.of("type","string","enum",Arrays.stream(c.getEnumConstants()).map(Object::toString).toList());
  Map<String,Object> properties=new LinkedHashMap<>();
  for(var r:c.getRecordComponents())properties.put(r.getName(),schema(r.getGenericType()));
  return Map.of("type","object","properties",properties,"required",new ArrayList<>(properties.keySet()),"additionalProperties",false);
 }
}
