const days=['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
const outcomes=['Yes','No','Partial','Not Applicable','Others'];
const labels={'Yes':'Done','No':'Missed','Partial':'Partial','Not Applicable':'Not applicable','Others':'Others','':'Awaiting feedback'};
const marks={'Yes':'✓','No':'✕','Partial':'◐','Not Applicable':'–','Others':'✎','':'◷'};
const displayDay=d=>new Date(d+'T12:00:00').toLocaleDateString([],{weekday:'long',month:'long',day:'numeric',year:'numeric'});
function outcome(e){return ({'Done on time':'Yes','Done late':'Yes','Partly':'Partial','Not done':'No','Other':'Others'})[e.outcome]||e.outcome;}
function pendingEntries(){return state.entries.filter(e=>!e.outcome).sort((a,b)=>a.due-b.due);}
function age(e){
 const elapsed=Math.round((Date.parse(today())-Date.parse(e.day))/86400000);
 return elapsed===0?'Due today':elapsed===1?'Due yesterday':`Due ${elapsed} days ago`;
}
function pendingPage(){
 reload();const entries=pendingEntries();
 shell('pending',`${backBtn}<h1>Awaiting feedback · ${entries.length}</h1>${entries.map(e=>`<button class="card" onclick="goEntry(${e.id})"><strong>${esc(e.title)}</strong><small>${age(e)} · ${stamp(e.due)}</small></button>`).join('')||'<p>You’re all caught up.</p>'}`);
}
function home(){
 reload();const pending=pendingEntries();
 shell('home',`<p class="sub">${displayDay(today())}</p><h1>Remindo</h1>
 <button class="card yellow" onclick="pendingPage()"><strong>Awaiting feedback · ${pending.length} ›</strong><small>${pending.length?'Oldest check-in: '+age(pending[0]).toLowerCase():'You’re all caught up.'}</small></button>${art('water')}
 ${(!state.alerts||!state.exact)?'<button class="card yellow" onclick="settings()">Set up reminder delivery ›</button>':''}
 <div class="row"><h2>My reminders</h2><button class="link" onclick="editor()">＋ New</button></div>
 ${state.goals.filter(g=>g.next<8.64e15).map(g=>`<button class="card pink" onclick="editor(${g.id})"><strong>${esc(g.title)}</strong><small>Next scheduled alert · ${stamp(g.next)}</small><small>${g.unit==='once'?'One-off task':'Recurring task'} · ${g.checkDays==='legacyEach'?'Check-in with each occurrence':'Feedback at '+esc(g.feedback)}</small></button>`).join('')||'<button class="card pink" onclick="editor()">Create a reminder →</button>'}`);
}
function options(items,value){return items.map(([v,t])=>`<option value="${v}" ${v==value?'selected':''}>${t}</option>`).join('');}
function weekdayPicker(id,mask){return `<div class="weekdays" id="${id}">${days.map((d,i)=>`<label><input type="checkbox" value="${1<<i}" ${mask&(1<<i)?'checked':''}>${d}</label>`).join('')}</div>`;}
function mask(id){return Array.from($(id).querySelectorAll('input:checked')).reduce((n,x)=>n+Number(x.value),0);}
function editor(id=0,preset='water'){
 const water=preset==='water';
 const g=state.goals.find(x=>x.id===id)||{title:water?'Drink 2 liters of water':preset==='email'?'Check my email':'',anchor:today(),step:1,unit:'days',start:'09:00',feedback:'20:00',weekdays:127,nudgeCount:water?4:1,gap:water?180:60,nudgeMask:127,nudgeDays:'first',checkDays:'first',checkWeekday:7};
 shell('edit',`${backBtn}<h1>${id?'Edit':'New'} reminder</h1>${id?'':`<div class="chips">${['water','email','custom'].map(x=>`<button class="${preset===x?'active':''}" onclick="editor(0,'${x}')">${{water:'Water',email:'Email',custom:'Custom'}[x]}</button>`).join('')}</div>`}
 <form id="goalform"><label for="title">Task</label><input id="title" required maxlength="120" value="${esc(g.title)}">
 <fieldset><legend>Task schedule</legend><label for="unit">Repeat</label><select id="unit">${options([['once','One time only'],['days','Daily'],['weeks','Weekly'],['months','Monthly']],g.unit)}</select>
 <div id="stepBox"><label id="stepLabel" for="step"></label><input type="number" min="1" max="365" id="step" value="${g.step}"></div>
 <label for="anchor">Starting date</label><input type="date" required id="anchor" value="${g.anchor}"><div id="weekBox"><label>Task days</label>${weekdayPicker('weekdays',g.weekdays)}</div></fieldset>
 <fieldset><legend>Nudges</legend><label for="nudging">Remind me to do it</label><select id="nudging">${options([['yes','Send nudges'],['no','No nudges']],g.nudgeCount?'yes':'no')}</select>
 <div id="nudgeBox"><div id="nudgeDayBox"><label for="nudgeDays">Nudge days in each cycle</label><select id="nudgeDays">${options([['first','Only on the starting day'],['selected','On selected weekdays']],g.nudgeDays)}</select>
 <div id="maskBox">${weekdayPicker('nudgeMask',g.nudgeMask)}</div></div>
 <div class="pair"><label>First nudge<input type="time" id="start" value="${g.start}"></label><label>Nudges per day<input type="number" min="1" max="144" id="count" value="${g.nudgeCount||1}"></label></div>
 <label id="gapBox">Minutes between nudges<input type="number" id="gap" min="1" max="21900" value="${g.gap}"></label></div></fieldset>
 <fieldset class="pink"><legend>Feedback</legend><div id="checkBox"><label for="checkDays">Check-in day</label><select id="checkDays">${options([['first','Starting day of each cycle'],['end','Last day of each cycle'],['weekday','First selected weekday in each cycle']],g.checkDays)}</select>
 <div id="checkWeekBox"><label for="checkWeekday">Weekday</label><select id="checkWeekday">${options(days.map((d,i)=>[i+1,d]),g.checkWeekday)}</select></div></div>
 <label for="feedback">Ask for feedback at</label><input type="time" required id="feedback" value="${g.feedback}"></fieldset>
 <button class="primary">Save reminder</button></form>${id?'<button class="link" id="archive">Archive reminder</button>':''}`);
 function fields(){
  const unit=$('unit').value,cycle=['weeks','months'].includes(unit);
  $('stepBox').hidden=unit==='once';$('stepLabel').textContent='Repeat every how many '+unit+'?';
  $('weekBox').hidden=unit!=='days';$('nudgeBox').hidden=$('nudging').value==='no';
  $('nudgeDayBox').hidden=$('checkBox').hidden=!cycle;
  $('maskBox').hidden=$('nudgeDays').value!=='selected';$('gapBox').hidden=Number($('count').value)<=1;
  $('checkWeekBox').hidden=$('checkDays').value!=='weekday';
 }
 $('goalform').oninput=fields;fields();
 $('goalform').onsubmit=ev=>{
  ev.preventDefault();
  const unit=$('unit').value,cycle=['weeks','months'].includes(unit),enabled=$('nudging').value==='yes';
  try{
   call('save',{id,title:$('title').value,unit,step:unit==='once'?1:Number($('step').value),anchor:$('anchor').value,
    weekdays:unit==='days'?mask('weekdays'):127,nudgeDays:cycle?$('nudgeDays').value:'first',
    nudgeMask:enabled&&cycle&&$('nudgeDays').value==='selected'?mask('nudgeMask'):127,nudgeCount:enabled?Number($('count').value):0,
    gap:enabled&&Number($('count').value)>1?Number($('gap').value):60,start:enabled?$('start').value:'09:00',
    checkDays:cycle?$('checkDays').value:'first',checkWeekday:Number($('checkWeekday').value),feedback:$('feedback').value});
   home();toast('Reminder saved');
  }catch(err){toast(err.message);}
 };
 if(id)$('archive').onclick=()=>{if(confirm('Stop future reminders and keep past feedback?')){call('archive',{id});home();}};
}

