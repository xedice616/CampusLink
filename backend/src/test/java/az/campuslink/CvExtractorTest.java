package az.campuslink;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class CvExtractorTest {
 final CvExtractor extractor=new CvExtractor(5*1024*1024);
 @Test void readsPdfAndDocx()throws Exception{
  assertTrue(extractor.extract(CareerFixtures.pdf()).contains("Java REST APIs"));
  assertTrue(extractor.extract(CareerFixtures.docx()).contains("Java REST APIs"));
 }
 @Test void rejectsSpoofedMimeSignatureAndMalformed()throws Exception{
  assertThrows(ApiException.class,()->extractor.extract(new MockMultipartFile("file","cv.exe","application/pdf",new byte[]{1})));
  assertThrows(ApiException.class,()->extractor.extract(new MockMultipartFile("file","cv.pdf","text/plain",CareerFixtures.pdf().getBytes())));
  assertThrows(ApiException.class,()->extractor.extract(new MockMultipartFile("file","cv.pdf","application/pdf","hello".getBytes())));
  assertThrows(ApiException.class,()->extractor.extract(new MockMultipartFile("file","cv.pdf","application/pdf","%PDF-1.7 broken".getBytes())));
  assertThrows(ApiException.class,()->new CvExtractor(20).extract(CareerFixtures.pdf()));
 }
 @Test void rejectsEmptyAndEncryptedPdf()throws Exception{
  assertThrows(ApiException.class,()->extractor.extract(new MockMultipartFile("file","cv.pdf","application/pdf",new byte[0])));
  try(var doc=new org.apache.pdfbox.pdmodel.PDDocument();var out=new ByteArrayOutputStream()){
   doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
   doc.protect(new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy("owner","secret",new org.apache.pdfbox.pdmodel.encryption.AccessPermission()));doc.save(out);
   assertThrows(ApiException.class,()->extractor.extract(new MockMultipartFile("file","cv.pdf","application/pdf",out.toByteArray())));
  }
 }
 @Test void rejectsZipBombAndMacroEntry()throws Exception{
  for(String name:new String[]{"word/vbaProject.bin","word/document.xml"}){
   var out=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry(name));zip.write(new byte[5*1024*1024]);zip.closeEntry();}
   assertThrows(ApiException.class,()->extractor.extract(new MockMultipartFile("file","cv.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",out.toByteArray())));
  }
 }
}
