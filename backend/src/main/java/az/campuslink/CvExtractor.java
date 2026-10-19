package az.campuslink;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

@Component
public class CvExtractor {
 private final long maxBytes;
 public CvExtractor(@Value("${career.max-file-bytes:5242880}") long maxBytes){this.maxBytes=maxBytes;}
 public long maxBytes(){return maxBytes;}
 private ApiException invalid(String message){return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,message);}
 public String extract(MultipartFile file){
  if(file==null||file.isEmpty())throw invalid("Select a PDF or DOCX resume");
  if(file.getSize()>maxBytes)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Resume exceeds configured file size limit");
  String name=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);
  String mime=Objects.toString(file.getContentType(),"").toLowerCase(Locale.ROOT);
  boolean pdf=name.endsWith(".pdf"),docx=name.endsWith(".docx");
  if((!pdf&&!docx)||(pdf&&!mime.equals("application/pdf"))||(docx&&!mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")))throw invalid("File extension and MIME type must match PDF or DOCX");
  try(InputStream input=file.getInputStream()){
   byte[] bytes=input.readNBytes((int)Math.min(maxBytes+1,Integer.MAX_VALUE));
   if(bytes.length>maxBytes)throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Resume exceeds configured file size limit");
   String text=pdf?pdf(bytes):docx(bytes);
   text=text.replace('\u0000',' ').strip();
   if(text.length()<80)throw invalid("Resume has too little readable text. Scanned/image PDFs require OCR, which is not supported.");
   if(text.length()>30000)throw invalid("Resume exceeds 30,000 extracted characters; shorten the document");
   return text;
  }catch(ApiException e){throw e;}catch(Exception e){throw invalid("Document could not be safely read. Use an unencrypted, text-based PDF or DOCX.");}
 }
 private String pdf(byte[] bytes)throws IOException{
  if(bytes.length<5||!new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))throw invalid("PDF signature does not match file type");
  try(var doc=Loader.loadPDF(bytes)){
   if(doc.isEncrypted()||!doc.getCurrentAccessPermission().canExtractContent())throw invalid("Encrypted or restricted PDFs are unsupported");
   if(doc.getNumberOfPages()>30)throw invalid("Resume must have at most 30 pages");
   var catalog=doc.getDocumentCatalog().getCOSObject();
   for(String key:List.of("OpenAction","AA","AcroForm"))if(catalog.containsKey(key))throw invalid("Active PDF content is unsupported");
   var names=doc.getDocumentCatalog().getNames();
   if(names!=null&&(names.getJavaScript()!=null||names.getEmbeddedFiles()!=null))throw invalid("PDF scripts and attachments are unsupported");
   // Text only: no rendering, links, actions or scripts are executed.
   var text=new StringBuilder();
   var bounded=new Writer(){
    public void write(char[] chars,int offset,int length){if(text.length()+length>30000)throw invalid("Resume exceeds 30,000 extracted characters");text.append(chars,offset,length);}
    public void flush(){}public void close(){}
   };
   new PDFTextStripper().writeText(doc,bounded);return text.toString();
  }
 }
 private String docx(byte[] bytes)throws IOException{
  if(bytes.length<4||bytes[0]!='P'||bytes[1]!='K'||bytes[2]!=3||bytes[3]!=4)throw invalid("DOCX ZIP signature does not match file type");
  boolean document=false,types=false;int count=0;long total=0;Set<String> entries=new HashSet<>();
  try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){
   ZipEntry entry;byte[] buffer=new byte[8192];
   while((entry=zip.getNextEntry())!=null){
    String name=entry.getName().toLowerCase(Locale.ROOT);
    if(++count>300||!entries.add(name)||name.contains("..")||name.startsWith("/")||name.contains("\\")||name.contains("vbaproject")||name.contains("embeddings/")||name.contains("activex/"))throw invalid("Unsafe or overly complex DOCX archive");
    document|=name.equals("word/document.xml");types|=name.equals("[content_types].xml");long size=0;int read;
    while((read=zip.read(buffer))!=-1){size+=read;total+=read;if(size>4*1024*1024||total>20*1024*1024)throw invalid("DOCX decompression limit exceeded");}
    if(entry.getCompressedSize()>0&&size>100000&&size/(double)entry.getCompressedSize()>100)throw invalid("DOCX compression ratio is unsafe");
   }
  }
  if(!document||!types)throw invalid("ZIP is not a Word DOCX document");
  try(var documentFile=new XWPFDocument(new ByteArrayInputStream(bytes));var extractor=new XWPFWordExtractor(documentFile)){
   return extractor.getText();
  }
 }
}
