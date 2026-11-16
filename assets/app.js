const $=id=>document.getElementById(id);
const esc=x=>String(x==null?'':x).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const dateKey=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
const today=()=>dateKey(new Date());
const clock=t=>new Date(t).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'});
const stamp=t=>new Date(t).toLocaleDateString([],{month:'short',day:'numeric'})+' · '+clock(t);
let state,page='home',origin='home',range='Week',from=today(),until=today(),messageTimer;
function call(action,p={}){const data=JSON.parse(Phone.call(action,JSON.stringify(p)));if(data.error)throw Error(data.error);return data;}
function toast(t){$('toast').textContent=t;clearTimeout(messageTimer);messageTimer=setTimeout(()=>$('toast').textContent='',4200);}
function reload(){state=call('state');}
function resume(route){reload();if(route&&route.startsWith('entry:'))return entry(Number(route.slice(6)));if(route==='history')return historyPage();if(route==='home'||page==='home')home();else if(page==='history')historyPage();else if(page==='settings')settings();}
function shell(name,html){page=name;$('app').innerHTML=html;$('nav').hidden=!['home','history','settings'].includes(page);$('nav').innerHTML=[['home','Today','home()'],['history','History','historyPage()'],['settings','Settings','settings()']].map(([n,title,fn])=>`<button class="${n===page?'on':''}" onclick="${fn}">${title}</button>`).join('');window.scrollTo(0,0);}
function back(){if(page==='home')return false;if(page==='entry'&&origin==='history')historyPage();else home();return true;}
const backBtn='<button class="back" onclick="back()">‹ Back</button>';
const art=(type)=>`<img class="art" alt="A little calico kitty with her water glass" src="${type}.jpg">`;
function goEntry(id){origin=page;entry(id);}
function entryCard(e){return `<button class="card" onclick="goEntry(${e.id})"><small>${stamp(e.due)}</small><strong>${esc(e.title)}</strong><span class="tag">${esc(e.outcome||'Awaiting feedback')}</span>${e.note?`<p class="note">${esc(e.note)}</p>`:''}</button>`;}
function home(){
 reload();const pending=state.entries.filter(e=>!e.outcome);
 shell('home',`<p class="sub">${new Date().toLocaleDateString([],{weekday:'long',month:'long',day:'numeric'})}</p><h1>Remindo</h1><div class="hero">${art('water')}</div>
 ${(!state.alerts||!state.exact)?'<button class="card yellow" onclick="settings()"><strong>Set up reminder delivery ›</strong><small>Check notification and alarm permissions</small></button>':''}
 <div class="row"><h2>My reminders</h2><button class="link" onclick="editor()">＋ New</button></div>
 ${state.goals.length?state.goals.map(g=>`<button class="card pink" onclick="editor(${g.id})"><strong>${esc(g.title)}</strong><small>Next nudge · ${stamp(g.next)}</small><small>${g.mode==='daily'?'Daily check-in · '+esc(g.feedback):'Check in with each nudge'}</small></button>`).join(''):'<button class="card pink" onclick="editor()"><strong>Create your first reminder →</strong><small>Start with water, email, or your own goal.</small></button>'}
 <h2>Waiting for feedback${pending.length?' · '+pending.length:''}</h2>${pending.length?pending.map(entryCard).join(''):'<p class="muted">You’re all caught up.</p>'}`);
}
function editor(id=0,preset='water'){
 const g=state.goals.find(x=>x.id===id)||{title:preset==='water'?'Drink 2 liters of water':preset==='email'?'Check my email':'',anchor:today(),step:preset==='water'?3:1,unit:preset==='water'?'hours':'days',start:'09:00',end:'18:00',feedback:'20:00',mode:preset==='water'?'daily':'each'};
 shell('edit',`${backBtn}<h1>${id?'Edit':'New'} reminder</h1>${id?'':`<div class="chips">${['water','email','custom'].map(x=>`<button class="${preset===x?'active':''}" onclick="editor(0,'${x}')">${{water:'Water',email:'Email',custom:'Custom'}[x]}</button>`).join('')}</div>`}
 <form id="goalform"><label for="title">Goal</label><input id="title" required maxlength="120" value="${esc(g.title)}" placeholder="What would you like to do?">
 <fieldset><legend>Remind me</legend><div class="pair"><div><label for="step">Every</label><input type="number" min="1" max="365" id="step" required value="${g.step}"></div><div><label for="unit">Unit</label><select id="unit">${['hours','days','weeks','months'].map(x=>`<option ${x===g.unit?'selected':''}>${x}</option>`).join('')}</select></div></div><label for="anchor">Starting date</label><input id="anchor" type="date" required value="${esc(g.anchor)}"><div class="pair"><div><label for="start">At</label><input type="time" id="start" required value="${g.start}"></div><div id="endbox"><label for="end">Until</label><input type="time" id="end" value="${g.end}"></div></div></fieldset>
 <fieldset class="pink"><legend>Ask for feedback</legend><select aria-label="Feedback schedule" id="mode"><option value="daily" ${g.mode==='daily'?'selected':''}>Once on each reminder day</option><option value="each" ${g.mode==='each'?'selected':''}>With every nudge</option></select><div id="fbox"><label for="feedback">Check-in time</label><input type="time" id="feedback" value="${g.feedback}"></div></fieldset><button class="primary">Save reminder</button></form>${id?'<button class="link" id="archive">Archive reminder</button>':''}`);
 function fields(){ $('endbox').hidden=$('unit').value!=='hours';$('fbox').hidden=$('mode').value!=='daily'; }
 $('unit').onchange=$('mode').onchange=fields;fields();
 $('goalform').onsubmit=e=>{e.preventDefault();try{let p={id};for(let k of ['title','unit','step','anchor','start','end','mode','feedback'])p[k]=$(k).value;call('save',p);home();toast('Reminder saved');}catch(err){toast(err.message);}};
 if(id)$('archive').onclick=()=>{if(confirm('Stop future reminders and keep past feedback?')){call('archive',{id});home();}};
}
function entry(id){
 reload();const e=state.entries.find(x=>x.id===id);if(!e)return home();
 const options=e.mode==='each'?['Done on time','Done late','Not done','Other']:['Yes','Partly','No','Other'];
 shell('entry',`${backBtn}<p class="sub">${stamp(e.due)}</p><h1>${e.mode==='daily'?'Daily check-in':'Reminder check-in'}</h1>${art('evening')}<h2>${esc(e.title)}</h2><p>${e.mode==='daily'?'Did you reach your goal that day?':'How did it go?'}</p><form id="answer">${options.map(v=>`<label class="choice ${e.outcome===v?'chosen':''}"><input required type="radio" name="outcome" value="${v}" ${e.outcome===v?'checked':''}>${v}</label>`).join('')}<label for="note">Comment (optional)</label><textarea maxlength="4000" id="note">${esc(e.note)}</textarea><button class="primary">Save feedback</button></form>${!e.outcome?'<button class="link" id="snooze">Remind me in 30 minutes</button>':''}`);
 $('answer').onchange=()=>{document.querySelectorAll('.choice').forEach(l=>l.classList.toggle('chosen',l.firstChild.checked));};
 $('answer').onsubmit=ev=>{ev.preventDefault();call('answer',{id,goal:e.goal,outcome:$('answer').elements.outcome.value,note:$('note').value.trim()});origin==='history'?historyPage():home();toast('Feedback saved');};
 if($('snooze'))$('snooze').onclick=()=>{call('snooze',{id,goal:e.goal});home();toast('Check-in snoozed');};
}
function historyPage(){
 reload();let a=new Date(),b=new Date();a.setHours(0,0,0,0);
 if(range==='Week')a.setDate(a.getDate()-6);
 if(range==='Last week'){a.setDate(a.getDate()-(a.getDay()+6)%7-7);b=new Date(a);b.setDate(b.getDate()+6);}
 if(range==='Month')a.setDate(1);
 let lo=range==='Custom'?from:dateKey(a),hi=range==='Custom'?until:dateKey(b);
 const entries=state.entries.filter(e=>e.day>=lo&&e.day<=hi);
 const done=entries.filter(e=>['Yes','Done on time','Done late'].includes(e.outcome)).length;
 const pending=entries.filter(e=>!e.outcome).length;
 shell('history',`<h1>History</h1><div class="chips">${['Week','Last week','Month','Custom'].map(r=>`<button onclick="range='${r}';historyPage()" class="${range===r?'active':''}">${r}</button>`).join('')}</div>${range==='Custom'?`<div class="pair range"><label>From<input type="date" id="from" max="${hi}" value="${lo}"></label><label>Through<input type="date" id="until" min="${lo}" value="${hi}"></label></div>`:`<p class="muted">${lo} — ${hi}</p>`}
 <div class="card yellow"><strong>${done} of ${entries.length} goals completed</strong><small>${pending} awaiting feedback · ${entries.filter(e=>e.outcome==='Partly').length} partly completed</small></div>${entries.length?entries.map(entryCard).join(''):'<p class="muted">No check-ins in this period.</p>'}`);
 if(range==='Custom'){ $('from').onchange=()=>{if($('from').value){from=$('from').value;historyPage();}};$('until').onchange=()=>{if($('until').value){until=$('until').value;historyPage();}};}
}
function settings(){
 reload();shell('settings',`<h1>Settings</h1><h2>Reminder delivery</h2><button class="card" onclick="call('notifications')"><strong>Notifications</strong><small>${state.alerts?'Enabled':'Tap to allow alerts'}</small></button><button class="card" ${state.exact?'disabled':''} onclick="call('exact')"><strong>Alarms &amp; reminders</strong><small>${state.exact?'Precise timing enabled':'Tap to allow precise timing'}</small></button><button class="card pink" onclick="call('weekly',{enabled:!state.weekly});settings()"><strong>Weekly review · ${state.weekly?'On':'Off'}</strong><small>Sunday at 6 PM</small></button><p class="muted">Unanswered check-ins repeat hourly. Follow-ups pause from 9 PM to 8 AM.</p>`);
}
setInterval(()=>{if(page==='home'&&!document.hidden){home();}},60000);

