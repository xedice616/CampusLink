const escape=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const categories={TECHNICAL_SKILLS:'Technical skill alignment',PROJECT_RELEVANCE:'Project relevance',RELEVANT_EXPERIENCE:'Relevant experience',EDUCATION_QUALIFICATIONS:'Education & qualifications',EVIDENCE_QUALITY:'Evidence quality',READABILITY_STRUCTURE:'Readability & structure'};
let account=null,version=0;
const fresh=()=>({file:null,role:'',job:'',language:'en',consent:false,save:false,busy:false,loading:false,error:'',message:'',access:null,result:null,preview:null,history:[]});
let model=fresh();
export function clearCareer(){account=null;version++;model=fresh();}
async function api(path,{method='GET',body}={}){
 const token=localStorage.getItem('cl-token');if(!token)throw Error('Please sign in to analyze your CV.');
 let response;try{response=await fetch('/api/v1/career'+path,{method,body,headers:{Authorization:'Bearer '+token}});}catch{throw Error('Cannot reach the backend. Check the Spring Boot server.');}
 const json=await response.json().catch(()=>null);
 if(!response.ok)throw Error(json?.message||'Request failed (HTTP '+response.status+')');return json;
}
const list=items=>items.length?'<ul>'+items.map(x=>'<li>'+escape(x)+'</li>').join('')+'</ul>':'<p class="career-muted">No items identified.</p>';
function evidence(title,items){return `<section class="career-card"><h3>${title}</h3>${items.length?items.map(x=>`<div class="career-fact"><strong>${escape(x.label)}</strong><blockquote>${escape(x.excerpt)}</blockquote></div>`).join(''):'<p class="career-muted">No supported evidence extracted.</p>'}</section>`;}
function results(result){
 const {analysis:a,matching:m}=result;
 return `<div class="career-result" aria-label="CV analysis results"><section class="career-overview"><div><span class="eyebrow">${m.scoreKind==='general-cv-quality'?'GENERAL CV QUALITY':'JOB COMPATIBILITY'}</span><h2>${escape(a.summary)}</h2><p>${escape(result.role||'Your next career step')}</p><small>${escape(result.provider)} · ${escape(result.model)} · ${escape(result.createdAt)}</small></div><div class="career-score"><strong>${m.overallScore??'—'}</strong><span>${m.overallScore===null?'Insufficient job requirements':'/ 100 · heuristic'}</span></div></section>
 <p class="career-disclaimer">Guidance for improving your CV. This is not a guaranteed ATS score, hiring probability or assessment of your worth.</p>
 <div class="career-grid"><section class="career-card"><h3>Category breakdown</h3>${m.categories.map(c=>`<div class="career-category"><div><b>${escape(categories[c.category])}</b><span>${c.score===null?'Not assessed':c.score+' / 100'}</span></div>${c.score!==null?`<meter min="0" max="100" value="${c.score}" aria-label="${escape(categories[c.category])}">${c.score}</meter>`:''}<small>${escape(c.explanation)}</small></div>`).join('')}<p class="career-muted">Weights: technical 30%, projects 20%, experience 20%, relevant education 10%, evidence 10%, structure 10%. Unassessed categories are excluded and weights renormalized. Quality scores use AI judgment.</p></section>
 <section class="career-card"><h3>Strengths</h3>${list(a.strengths)}<h3>Improvement priorities</h3>${list(a.priorities)}<h3>Missing information</h3>${list(a.missingInformation)}</section></div>
 ${a.requirements.length?`<section class="career-card"><h3>Requirement alignment & skills gaps</h3>${a.requirements.map(r=>`<article class="career-requirement"><div><h4>${escape(r.requirement)}</h4><span class="career-badge ${r.alignment.toLowerCase()}">${escape(r.alignment)}</span></div><p>${escape(r.explanation)}</p><small>Job description evidence</small><blockquote>${escape(r.jobEvidence)}</blockquote><small>CV evidence</small>${r.cvEvidence.length?r.cvEvidence.map(q=>'<blockquote>'+escape(q)+'</blockquote>').join(''):'<p class="career-muted">No evidence found in this CV.</p>'}</article>`).join('')}</section>`:m.scoreKind==='general-cv-quality'?'<section class="career-card"><h3>General analysis</h3><p>No job description was provided. Recommendations use your stated goal; no job requirements were invented.</p></section>':'<section class="career-card"><h3>Insufficient job requirements</h3><p>No supported requirements could be extracted from the supplied job description. No overall compatibility score was calculated.</p></section>'}
 <div class="career-grid">${evidence('Supported skills',a.skills)}${evidence('Projects',a.projects)}${evidence('Experience',a.experience)}${evidence('Education',a.education)}</div>
 <div class="career-grid"><section class="career-card"><h3>Skills to develop</h3>${list(a.skillsToDevelop)}<h3>Learning topics & courses to look for</h3>${list(a.learningTopics)}</section><section class="career-card"><h3>Portfolio project ideas</h3>${list(a.portfolioIdeas)}<h3>Interview preparation</h3>${list(a.interviewTopics)}</section></div>
 <section class="career-card"><h3>CV bullet improvements</h3>${a.bulletImprovements.length?a.bulletImprovements.map(b=>`<div class="career-fact"><small>Original evidence</small><blockquote>${escape(b.original)}</blockquote><small>Suggested wording — verify before using</small><p>${escape(b.improved)}</p></div>`).join(''):'<p class="career-muted">No suitable bullet points identified.</p>'}</section>
 <section class="career-card"><h3>Your 30 / 60 / 90-day roadmap</h3><div class="career-roadmap">${a.roadmap.map(r=>`<article><span class="eyebrow">DAY ${r.day}</span>${list(r.actions)}</article>`).join('')}</div></section>
 <section class="career-card"><h3>Confidence & limitations</h3><p>AI self-reported confidence: ${Math.round(a.confidence*100)}%. This is not a calibrated probability.</p>${list([...a.limitations,...result.limitations])}<p>${result.saved?'Saved privately until '+escape(result.expiresAt)+'.':'Not saved to the server. This result is held in page memory only.'}</p>${result.saved?`<button class="btn ghost" data-career-delete="${escape(result.id)}">Delete this saved result</button>`:''}</section></div>`;
}
function content(){
 const s=model,access=s.access,canRun=access?.providerConfigured&&(access.freeAvailable||access.pro)&&!access.busy;
 return `<header class="career-heading"><div><span class="eyebrow">CAMPUSLINK / CAREER INTELLIGENCE</span><h1>A clearer next step.</h1><p>Understand your CV, see the evidence, and build a practical path toward your career goals.</p></div><span class="career-badge">PRIVATE BY DEFAULT</span></header>
 <div class="career-access" role="status">${!access?'Checking your analysis access…':!access.providerConfigured?'AI unavailable: the server needs a provider, model and API key.':access.busy?'An analysis is already running for your account.':access.pro?'PRO entitlement active':access.freeAvailable?'Your account includes one free AI analysis.':'Your free analysis has been used. A valid PRO entitlement is required. Payments are not connected.'}<button type="button" class="text-link" data-career-refresh>Refresh access</button></div>
 <div class="career-workspace"><section class="career-card"><h2>Your CV & career goal</h2><form id="career-analysis-form" class="career-form"><fieldset ${s.busy?'disabled':''}><label class="career-upload" for="career-file"><span class="eyebrow">01 / RESUME</span><strong>${escape(s.file?.name||'Choose your CV')}</strong><span>PDF or DOCX · up to ${access?Math.round(access.maxFileBytes/1048576):5} MB · text-based documents</span><input id="career-file" type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"></label>
 <label>Target role or career goal<input name="role" maxlength="200" value="${escape(s.role)}" placeholder="e.g. Junior Java Developer"></label>
 <label>Job description <span class="career-muted">optional</span><textarea name="jobDescription" maxlength="12000" rows="6" placeholder="Paste the job description for an evidence-based comparison. Leave empty for general CV quality coaching.">${escape(s.job)}</textarea></label>
 <label>AI response language<select name="language">${[['az','Azərbaycan dili'],['en','English'],['ru','Русский'],['tr','Türkçe'],['de','Deutsch']].map(([v,l])=>`<option value="${v}" ${s.language===v?'selected':''}>${l}</option>`).join('')}</select></label>
 <button type="button" class="btn ghost" data-career-preview ${!s.file?'disabled':''}>Preview text & privacy redaction</button>
 <label class="career-check"><input type="checkbox" name="consent" ${s.consent?'checked':''} required><span>I consent to sending extracted CV text, my career goal and job description to <strong>${escape(access?.provider||'the configured AI provider')}</strong> for this analysis. Contact details are automatically reduced, but identifying information may remain. I have reviewed my document and can remove sensitive details first.</span></label>
 <label class="career-check"><input type="checkbox" name="saveHistory" ${s.save?'checked':''}><span>Save my analysis privately for ${access?.retentionDays||30} days. Otherwise, results stay in page memory only. The original CV file and extracted input are never stored by CampusLink.</span></label>
 <button type="submit" class="btn wide" ${!canRun||!s.file?'disabled':''}>${s.busy?'Analysis in progress…':'Analyze my CV'}</button></fieldset></form>
 <div class="career-feedback" aria-live="polite" aria-atomic="true">${s.error?`<p class="career-error" role="alert">${escape(s.error)}</p>`:''}${s.message?`<p>${escape(s.message)}</p>`:''}${s.busy?'<p class="career-progress">Validating your document and waiting for AI analysis. No result is shown until validation completes.</p>':''}</div></section>
 <aside><section class="career-card"><span class="eyebrow">EVIDENCE FIRST</span><h2>Advice you can inspect.</h2><p>We separate CV facts from learning suggestions. Every extracted skill and matched requirement includes a source excerpt.</p><p>Without a job description, you receive general CV quality analysis and career coaching.</p><p class="career-muted">Scores are heuristic guidance. AI can make mistakes. Visual page design and scanned documents are not assessed.</p></section>
 <section class="career-card"><h3>Your saved analyses</h3>${s.history.length?s.history.map(h=>`<div class="career-history"><button type="button" class="text-link" data-career-open="${escape(h.id)}">${escape(new Date(Number(h.createdAt)).toLocaleString())}</button><button type="button" class="text-link" data-career-delete="${escape(h.id)}">Delete</button></div>`).join(''):'<p class="career-muted">No saved analyses. Saving is optional.</p>'}<button type="button" class="btn ghost" data-career-delete-all>Delete all career results / cancel pending analysis</button><p class="career-muted">Deletion does not reset your free quota. Already submitted provider requests cannot be recalled. OpenAI API retention depends on your provider account; store=false does not guarantee zero retention.</p></section></aside></div>
 ${s.preview?`<section class="career-card"><h3>Text privacy preview</h3><p>${escape(s.preview.disclosure)}</p><textarea class="career-preview" readonly rows="10" aria-label="Redacted CV text preview">${escape(s.preview.redactedText)}</textarea></section>`:''}
 ${s.result?results(s.result):'<div class="career-empty"><h2>Your next chapter starts with evidence.</h2><p>Upload a readable CV to receive your skills analysis, improvement priorities and learning roadmap.</p></div>'}`;
}
export function careerPage(){return '<div class="career-engine">'+content()+'</div>';}
function restoreFile(root){if(model.file){const transfer=new DataTransfer();transfer.items.add(model.file);root.querySelector('#career-file').files=transfer.files;}}
function redraw(){const root=document.querySelector('.career-engine');if(root){root.innerHTML=content();restoreFile(root);}}
async function refresh(){
 const generation=version;if(model.loading)return;model.loading=true;
 try{const [access,history]=await Promise.all([api('/access'),api('/analyses')]);if(generation!==version)return;model.access=access;model.history=history;}
 catch(e){if(generation===version)model.error=e.message;}finally{if(generation===version){model.loading=false;redraw();}}
}
export function mountCareer(user,language){
 if(account!==user?.id){clearCareer();account=user?.id;model.role=user?.careerGoal||'';model.language=['az','en','ru','tr','de'].includes(language)?language:'en';redraw();}
 const root=document.querySelector('.career-engine');if(!root)return;
 restoreFile(root);
 if(!root.dataset.bound){root.dataset.bound='true';
  root.addEventListener('input',e=>{if(e.target.name==='role')model.role=e.target.value;if(e.target.name==='jobDescription')model.job=e.target.value;});
  root.addEventListener('change',e=>{
   if(e.target.id==='career-file'){const file=e.target.files?.[0];model.error='';model.preview=null;model.consent=false;
    if(file&&(!/\.(pdf|docx)$/i.test(file.name)||file.size>(model.access?.maxFileBytes||5242880))){model.file=null;model.error='Choose a PDF or DOCX within the file size limit.';}else model.file=file||null;redraw();}
   if(e.target.name==='language')model.language=e.target.value;if(e.target.name==='consent')model.consent=e.target.checked;if(e.target.name==='saveHistory')model.save=e.target.checked;
  });
  root.addEventListener('submit',async e=>{
   if(e.target.id!=='career-analysis-form')return;e.preventDefault();if(model.busy||!model.file)return;
   const generation=version;const data=new FormData();data.set('file',model.file);data.set('role',model.role);data.set('jobDescription',model.job);data.set('language',model.language);data.set('consent',String(model.consent));data.set('saveHistory',String(model.save));
   model.busy=true;model.error='';model.message='';model.result=null;redraw();
   try{const result=await api('/analyze',{method:'POST',body:data});if(generation===version){model.result=result;model.message='Analysis complete. Review the evidence and limitations below.';}}
   catch(error){if(generation===version)model.error=error.message;}
   finally{if(generation===version){model.busy=false;model.consent=false;redraw();await refresh();}}
  });
  root.addEventListener('click',async e=>{
   const button=e.target.closest('button');if(!button||!button.matches('[data-career-refresh],[data-career-preview],[data-career-open],[data-career-delete],[data-career-delete-all]'))return;
   const generation=version;model.error='';
   try{
    if(button.hasAttribute('data-career-refresh')){await refresh();return;}
    if(button.hasAttribute('data-career-preview')&&model.file){button.disabled=true;model.message='Extracting a local privacy preview; nothing is being sent to AI.';const data=new FormData();data.set('file',model.file);const preview=await api('/preview',{method:'POST',body:data});if(generation===version){model.preview=preview;model.message='Privacy preview ready. Review identifying details before consenting.';}}
    if(button.dataset.careerOpen){const result=await api('/analyses/'+encodeURIComponent(button.dataset.careerOpen));if(generation===version)model.result=result;}
    if(button.dataset.careerDelete){await api('/analyses/'+encodeURIComponent(button.dataset.careerDelete),{method:'DELETE'});if(generation===version){if(model.result?.id===button.dataset.careerDelete)model.result=null;await refresh();}}
    if(button.hasAttribute('data-career-delete-all')){await api('/analyses',{method:'DELETE'});if(generation===version){version++;model.loading=false;model.busy=false;model.result=null;model.preview=null;model.file=null;model.consent=false;model.message='Career results deleted. Pending result persistence canceled. Your quota is unchanged.';await refresh();}}
   }catch(error){if(generation===version)model.error=error.message;}finally{redraw();}
  });
 }
 if(!model.access&&!model.loading)refresh();
}
