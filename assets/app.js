const $=id=>document.getElementById(id);
const esc=x=>String(x==null?'':x).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const dateKey=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
const today=()=>dateKey(new Date());
const clock=t=>new Date(t).toLocaleTimeString([],{hour:'numeric',minute:'2-digit'});
const stamp=t=>new Date(t).toLocaleDateString([],{month:'short',day:'numeric'})+' · '+clock(t);
let state,page='home',origin='home',range='Day',from=today(),until=today(),messageTimer;
function call(action,p={}){const data=JSON.parse(Phone.call(action,JSON.stringify(p)));if(data.error)throw Error(data.error);return data;}
function toast(t){$('toast').textContent=t;clearTimeout(messageTimer);messageTimer=setTimeout(()=>$('toast').textContent='',4200);}
function reload(){state=call('state');}
function resume(route){reload();if(route&&route.startsWith('entry:'))return entry(Number(route.slice(6)));if(route==='pending')return pendingPage();if(route==='history')return historyPage();if(route==='home'||page==='home')home();else if(page==='history')historyPage();else if(page==='settings')settings();}
function shell(name,html){page=name;$('app').innerHTML=html;$('nav').hidden=!['home','history','settings'].includes(page);$('nav').innerHTML=[['home','Today','home()'],['history','History','historyPage()'],['settings','Settings','settings()']].map(([n,title,fn])=>`<button class="${n===page?'on':''}" onclick="${fn}">${title}</button>`).join('');window.scrollTo(0,0);}
function back(){if($('reportDetail')){$('reportDetail').remove();return true;}if(page==='home')return false;if(page==='entry'&&origin==='pending'){pendingPage();return true;}if(page==='entry'&&origin==='history')historyPage();else home();return true;}
const backBtn='<button class="back" onclick="back()">‹ Back</button>';
const art=(type)=>`<img class="art" alt="A little calico kitty with her water glass" src="${type}.jpg">`;
function goEntry(id){origin=page;entry(id);}
function entry(id){
 reload();const e=state.entries.find(x=>x.id===id);if(!e)return home();
 const options=outcomes;
 shell('entry',`${backBtn}<p class="sub">${stamp(e.due)}</p><h1>${e.mode==='daily'?'Daily check-in':'Reminder check-in'}</h1>${art('evening')}<h2>${esc(e.title)}</h2><p>${e.mode==='daily'?'Did you reach your goal that day?':'How did it go?'}</p><form id="answer">${options.map(v=>`<label class="choice ${outcome(e)===v?'chosen':''}"><input required type="radio" name="outcome" value="${v}" ${outcome(e)===v?'checked':''}>${v}</label>`).join('')}<label for="note">Comment (optional)</label><textarea maxlength="4000" id="note">${esc(e.note)}</textarea><button class="primary">Save feedback</button></form>${!e.outcome?'<button class="link" id="snooze">Remind me in 30 minutes</button>':''}`);
 $('answer').onchange=()=>{document.querySelectorAll('.choice').forEach(l=>l.classList.toggle('chosen',l.firstChild.checked));};
 $('answer').onsubmit=ev=>{ev.preventDefault();call('answer',{id,goal:e.goal,outcome:$('answer').elements.outcome.value,note:$('note').value.trim()});origin==='history'?historyPage():origin==='pending'?pendingPage():home();toast('Feedback saved');};
 if($('snooze'))$('snooze').onclick=()=>{call('snooze',{id,goal:e.goal});home();toast('Check-in snoozed');};
}
function settings(){
 reload();shell('settings',`<h1>Settings</h1><h2>Reminder delivery</h2><button class="card" onclick="call('notifications')"><strong>Notifications</strong><small>${state.alerts?'Enabled':'Tap to allow alerts'}</small></button><button class="card" ${state.exact?'disabled':''} onclick="call('exact')"><strong>Alarms &amp; reminders</strong><small>${state.exact?'Precise timing enabled':'Tap to allow precise timing'}</small></button><button class="card pink" onclick="call('weekly',{enabled:!state.weekly});settings()"><strong>Weekly review · ${state.weekly?'On':'Off'}</strong><small>Sunday at 6 PM</small></button><p class="muted">Older check-ins and larger backlogs get one grouped follow-up a day. Follow-ups pause from 9 PM to 8 AM.</p>`);
}
setInterval(()=>{if(page==='home'&&!document.hidden){home();}},60000);

