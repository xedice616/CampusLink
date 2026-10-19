import {test,expect} from '@playwright/test';
// Synthetic browser fixtures only: all API responses below are explicitly intercepted.
const user={id:1,fullName:'Fixture Student',email:'fixture@example.test',careerGoal:'Java Developer',skills:[]};
const evidence={label:'Java',excerpt:'Built Java REST APIs'};
const result={id:'fixture-result',type:'ai-cv-analysis',provider:'fixture-provider',model:'fixture-model',language:'en',createdAt:'2026-10-19T00:00:00Z',saved:false,role:'Java Developer',matching:{overallScore:75,scoreKind:'general-cv-quality',categories:[{category:'EVIDENCE_QUALITY',score:75,weight:10,explanation:'Synthetic evidence fixture'}]},analysis:{summary:'Synthetic CV analysis fixture',skills:[evidence],projects:[evidence],education:[],experience:[],requirements:[],missingInformation:['Project dates'],strengths:['Project evidence'],priorities:['Add dates'],skillsToDevelop:['Testing'],learningTopics:['Integration testing'],portfolioIdeas:['Add endpoint tests'],bulletImprovements:[{original:'Built Java REST APIs',improved:'Built REST APIs using Java.'}],interviewTopics:['REST design'],roadmap:[{day:30,actions:['Document project']},{day:60,actions:['Add tests']},{day:90,actions:['Prepare walkthrough']}],confidence:0.7,limitations:['Synthetic fixture']},limitations:['AI can be wrong.']};
async function setup(page,{configured=true,failure=false}={}){
 let used=false,submitted=0;
 await page.addInitScript(()=>{localStorage.setItem('cl-token','a'.repeat(64));localStorage.setItem('cl-language','en');});
 await page.route('**/api/v1/**',async route=>{
  const path=new URL(route.request().url()).pathname;
  let body=[];let status=200;
  if(path==='/api/v1/auth/me')body=user;
  else if(path==='/api/v1/users')body=[user];
  else if(path==='/api/v1/preferences')body={saved:[],joined:[],exchanges:[]};
  else if(path==='/api/v1/career/access')body={freeAvailable:!used,pro:false,busy:false,providerConfigured:configured,provider:'fixture-provider',model:'fixture-model',retentionDays:30,maxFileBytes:5242880};
  else if(path==='/api/v1/career/preview')body={redactedText:'[NAME] Built Java REST APIs',disclosure:'Synthetic privacy preview. No remote AI call.'};
  else if(path==='/api/v1/career/analyze'){submitted++;await new Promise(r=>setTimeout(r,200));if(failure){status=502;body={message:'AI provider request failed. No quota was consumed.'};}else{used=true;body=result;}}
  else if(path==='/api/v1/career/analyses')body=route.request().method()==='DELETE'?{deleted:true,quotaReset:false}:[];
  else if(path==='/api/v1/auth/logout')body={message:'Logged out'};
  await route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto('/');await expect(page.getByRole('button',{name:'Log out',exact:true})).toBeVisible();if(page.viewportSize().width<700)await page.getByRole('button',{name:'Open menu'}).click();await page.getByRole('button',{name:'AI Career Assistant',exact:true}).click();await expect(page.locator('.career-access')).toContainText(configured?'one free':'AI unavailable');
 return ()=>submitted;
}
async function upload(page){await page.locator('#career-file').setInputFiles({name:'synthetic-test.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF- synthetic browser fixture; API is intercepted')});}
test('upload, private preview, required consent and validated result view',async({page})=>{
 const submitted=await setup(page);await upload(page);
 await page.getByRole('button',{name:'Preview text & privacy redaction'}).click();await expect(page.getByRole('textbox',{name:'Redacted CV text preview'})).toHaveValue('[NAME] Built Java REST APIs');
 expect(await page.locator('#career-file').evaluate(input=>input.files[0]?.name)).toBe('synthetic-test.pdf');
 await page.getByRole('button',{name:'Analyze my CV',exact:true}).click();expect(submitted()).toBe(0);
 await page.locator('[name=consent]').check();await page.getByRole('button',{name:'Analyze my CV',exact:true}).click();
 await expect(page.locator('.career-progress')).toBeVisible();await expect(page.getByText('Synthetic CV analysis fixture',{exact:true})).toBeVisible();
 await expect(page.getByRole('heading',{name:'Your 30 / 60 / 90-day roadmap'})).toBeVisible();await expect(page.locator('.career-access')).toContainText('valid PRO');expect(submitted()).toBe(1);
 await expect(page.locator('.career-engine img')).toHaveCount(0);
 await page.getByRole('button',{name:'Delete all career results / cancel pending analysis'}).click();await expect(page.locator('.career-result')).toHaveCount(0);await expect(page.locator('.career-access')).toContainText('valid PRO');
});
test('provider errors leave explicit error and allow retry without fabricated results',async({page})=>{
 await setup(page,{failure:true});await upload(page);await page.locator('[name=consent]').check();await page.getByRole('button',{name:'Analyze my CV',exact:true}).click();
 await expect(page.getByRole('alert')).toContainText('No quota was consumed');await expect(page.locator('.career-result')).toHaveCount(0);await expect(page.locator('.career-access')).toContainText('one free');
});
test('unconfigured provider disables analysis, supports five response languages and mobile layout',async({page})=>{
 await page.setViewportSize({width:390,height:844});await setup(page,{configured:false});await upload(page);
 await expect(page.getByRole('button',{name:'Analyze my CV',exact:true})).toBeDisabled();await expect(page.locator('[name=language] option')).toHaveCount(5);
 const overflow=await page.evaluate(()=>[...document.querySelectorAll('body *')].filter(e=>e.getBoundingClientRect().right>window.innerWidth+1).map(e=>({tag:e.tagName,cls:e.className,right:Math.round(e.getBoundingClientRect().right)})).slice(0,12));expect(overflow).toEqual([]);
});
test('invalid files are rejected and leaving/signing out preserves existing navigation',async({page})=>{
 await setup(page);await page.locator('#career-file').setInputFiles({name:'bad.exe',mimeType:'application/octet-stream',buffer:Buffer.from('bad')});
 await expect(page.getByRole('alert')).toContainText('PDF or DOCX');
 await page.getByRole('button',{name:'Courses',exact:true}).first().click();await expect(page.getByRole('heading',{name:'Courses',exact:true})).toBeVisible();
 await page.getByRole('button',{name:'Log out',exact:true}).click();await expect(page.getByRole('heading',{name:'Welcome Back!'})).toBeVisible();
});
