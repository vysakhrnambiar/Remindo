let selectedDay=today();
const reportOrder=['Yes','No','Partial','Not Applicable','','Others'];
function entryList(e){
 const v=outcome(e);
 return `<li><button class="result" onclick="goEntry(${e.id})"><span class="symbol s${reportOrder.indexOf(v)}">${marks[v]}</span><span><b>${esc(e.title)}</b><small>${labels[v]}${e.mode==='each'?' · '+clock(e.due):''}${e.outcome==='Done late'?' · Done late':''}</small>${e.note?`<span class="note">${esc(e.note)}</span>`:''}</span></button></li>`;
}
function dayReport(day,entries){
 const relevant=entries.filter(e=>outcome(e)!=='Not Applicable');
 let status='No check-ins';
 if(entries.length){
  if(!relevant.length)status='Not applicable';
  else if(relevant.every(e=>outcome(e)==='Yes'))status='All done';
  else if(relevant.every(e=>!e.outcome))status='Awaiting feedback';
  else if(relevant.some(e=>['Yes','Partial'].includes(outcome(e))))status='Almost there';
  else status='Daily results';
 }
 return `<h2>Daily recap</h2><p>${displayDay(day)}</p><span class="tag">${status}</span><ul class="results">${reportOrder.map(v=>entries.filter(e=>outcome(e)===v).map(entryList).join('')).join('')}</ul>`;
}
function detail(entries,title){
 const overlay=document.createElement('section');
 overlay.className='overlay';overlay.id='reportDetail';overlay.setAttribute('role','dialog');overlay.setAttribute('aria-label',title);
 overlay.innerHTML=`<article><button class="back" id="closeDetail">‹ Back to report</button><h2>${esc(title)}</h2>${entries.map(e=>`<div class="card"><strong>${displayDay(e.day)} · ${clock(e.due)}</strong><small>${esc(e.outcome||'Awaiting feedback')}</small><p class="note">${esc(e.note||'No comment added.')}</p>${!e.outcome?`<button onclick="goEntry(${e.id})">Give feedback</button>`:''}</div>`).join('')}</article>`;
 $('app').appendChild(overlay);$('closeDetail').onclick=()=>overlay.remove();$('closeDetail').focus();
}
let reportEntries=[];
function showGroup(goal,index){
 const v=reportOrder[index];
 const entries=reportEntries.filter(e=>e.goal===goal&&outcome(e)===v);
 if(entries.length)detail(entries,entries[0].title+' · '+labels[v]);
}
function rangeReport(entries){
 const groups=new Map();
 for(const e of entries){
  if(!groups.has(e.goal))groups.set(e.goal,{title:e.title,byStatus:new Map()});
  const group=groups.get(e.goal),v=outcome(e);
  if(!group.byStatus.has(v))group.byStatus.set(v,new Set());
  group.byStatus.get(v).add(e.day);
 }
 return Array.from(groups,([goal,g])=>`<article class="card"><h3>${esc(g.title)}</h3><div class="chips">${reportOrder.filter(v=>g.byStatus.has(v)).map(v=>{
 const n=g.byStatus.get(v).size;return `<button onclick="showGroup(${goal},${reportOrder.indexOf(v)})">${marks[v]} ${labels[v]} · ${n} ${n===1?'day':'days'}</button>`;
 }).join('')}</div></article>`).join('')||'<p class="muted">No check-ins in this period.</p>';
}
function historyPage(){
 reload();let a=new Date(),b=new Date();a.setHours(0,0,0,0);
 if(range==='Week')a.setDate(a.getDate()-6);
 if(range==='Last week'){a.setDate(a.getDate()-(a.getDay()+6)%7-7);b=new Date(a);b.setDate(b.getDate()+6);}
 if(range==='Month')a.setDate(1);
 const lo=range==='Day'?selectedDay:range==='Custom'?from:dateKey(a),hi=range==='Day'?selectedDay:range==='Custom'?until:dateKey(b);
 reportEntries=state.entries.filter(e=>e.day>=lo&&e.day<=hi);
 shell('history',`<h1>History</h1><div class="chips">${['Day','Week','Last week','Month','Custom'].map(r=>`<button onclick="range='${r}';historyPage()" class="${range===r?'active':''}">${r}</button>`).join('')}</div>
 ${range==='Day'?`<label>Choose a day<input type="date" id="chosenDay" value="${lo}"></label>`:range==='Custom'?`<div class="pair range"><label>From<input type="date" id="from" value="${lo}"></label><label>Through<input type="date" id="until" value="${hi}"></label></div>`:`<p class="muted">${lo} — ${hi}</p>`}
 ${lo===hi?dayReport(lo,reportEntries):rangeReport(reportEntries)}`);
 if(range==='Day')$('chosenDay').onchange=()=>{if($('chosenDay').value){selectedDay=$('chosenDay').value;historyPage();}};
 if(range==='Custom'){
  $('from').onchange=()=>{if($('from').value){from=$('from').value;if(until<from)until=from;historyPage();}};
  $('until').onchange=()=>{if($('until').value){until=$('until').value;if(from>until)from=until;historyPage();}};
 }
}

