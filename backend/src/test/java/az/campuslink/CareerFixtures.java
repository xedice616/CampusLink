package az.campuslink;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.util.*;
import static az.campuslink.CareerModels.*;

/** Clearly synthetic test resumes and AI output; not used by runtime. */
final class CareerFixtures {
 static final String TEXT="Built Java REST APIs with Spring Boot and SQL for a campus project. Tested endpoints using JUnit. Education: Computer Science student. Projects: campus team platform.";
 static final String JD="Must build Java REST APIs. SQL is required.";
 static MockMultipartFile pdf()throws Exception{
  try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){
   var page=new PDPage();doc.addPage(page);
   try(var content=new PDPageContentStream(doc,page)){
    content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),10);content.newLineAtOffset(20,700);content.showText(TEXT);content.endText();
   }
   doc.save(out);return new MockMultipartFile("file","resume.pdf","application/pdf",out.toByteArray());
  }
 }
 static MockMultipartFile docx()throws Exception{
  try(var doc=new XWPFDocument();var out=new ByteArrayOutputStream()){
   doc.createParagraph().createRun().setText(TEXT);doc.write(out);
   return new MockMultipartFile("file","resume.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",out.toByteArray());
  }
 }
 static Draft draft(boolean targeted){
  return new Draft("Synthetic test analysis",List.of(new Evidence("Java","Java REST APIs")),List.of(new Evidence("Student","Computer Science student")),
   List.of(new Evidence("Campus project","campus team platform")),List.of(),
   targeted?List.of(new Requirement(Category.TECHNICAL_SKILLS,"Java REST APIs","Must build Java REST APIs.",Alignment.MATCHED,List.of("Java REST APIs"),"Supported by project text"),
     new Requirement(Category.TECHNICAL_SKILLS,"SQL","SQL is required.",Alignment.PARTIAL,List.of("SQL"),"Listed without details")):List.of(),
   List.of(new Quality(Category.EVIDENCE_QUALITY,70,"Add measurements",List.of("Built Java REST APIs")),new Quality(Category.READABILITY_STRUCTURE,80,"Readable text",List.of("Education: Computer Science student"))),
   List.of("Project dates"),List.of("Project evidence"),List.of("Add dates"),List.of("Testing"),List.of("Integration testing"),List.of("Add tests to existing API"),
   List.of(new Bullet("Tested endpoints using JUnit.","Tested API endpoints using JUnit.")),List.of("Discuss REST API design"),
   List.of(new Roadmap(30,List.of("Add project documentation")),new Roadmap(60,List.of("Add integration tests")),new Roadmap(90,List.of("Practice project walkthrough"))),0.7,List.of("Synthetic fixture; not a real AI response"));
 }
}
