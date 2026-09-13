/* Vakit+ Owner Backend v1
 * Node.js 20+; HTTPS should be terminated by a reverse proxy (Caddy/Nginx/Cloud Run/etc.).
 * For production, replace JSON persistence with a managed database and put secrets in env vars.
 */
const http = require('http');
const express = require('express');
const rateLimit = require('express-rate-limit');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
let firebaseAdmin = null;
try { firebaseAdmin = require('firebase-admin'); } catch (_) {}

const PORT = Number(process.env.PORT || 8080);
const RATE_WINDOW_MS = 15 * 60 * 1000;
const LOGIN_LIMIT = 8;
const loginAttempts = new Map();
function rateLimitLogin(ip) {
  const now = Date.now();
  const item = loginAttempts.get(ip);
  if (!item || now - item.startedAt > RATE_WINDOW_MS) { loginAttempts.set(ip, {startedAt: now, count: 1}); return true; }
  item.count += 1;
  return item.count <= LOGIN_LIMIT;
}
function cleanupRateLimits() { const now=Date.now(); for (const [k,v] of loginAttempts) if(now-v.startedAt>RATE_WINDOW_MS) loginAttempts.delete(k); }
setInterval(cleanupRateLimits, RATE_WINDOW_MS).unref();
const HOST = process.env.HOST || '0.0.0.0';
const OWNER_EMAIL = (process.env.OWNER_EMAIL || '').trim().toLowerCase();
const OWNER_PASSWORD = process.env.OWNER_PASSWORD || '';
const JWT_SECRET = process.env.JWT_SECRET || '';
const FCM_PROJECT_ID = process.env.FCM_PROJECT_ID || '';
if (firebaseAdmin && FCM_PROJECT_ID) { firebaseAdmin.initializeApp({ credential: firebaseAdmin.credential.applicationDefault(), projectId: FCM_PROJECT_ID }); }
const DATA_FILE = process.env.DATA_FILE || path.join(__dirname, 'data.json');
if (!OWNER_EMAIL || !OWNER_PASSWORD || JWT_SECRET.length < 32) {
  console.error('OWNER_EMAIL, OWNER_PASSWORD ve en az 32 karakter JWT_SECRET zorunludur.');
  process.exit(1);
}

function load() {
  if (!fs.existsSync(DATA_FILE)) {
    const initial = { content: { appSubtitle:'Modern İslami yaşam asistanınız', dailyMessage:'Hoş geldiniz.', adminNote:'', version:1 }, audit: [], notifications: [], notificationDrafts: [], scheduledNotifications: [], devices: [], system: { maintenance:false, announcement:'', updatedAt:Date.now() } };
    fs.writeFileSync(DATA_FILE, JSON.stringify(initial, null, 2));
    return initial;
  }
  return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
}
let db = load();
if (!db.system) db.system={maintenance:false,announcement:'',updatedAt:Date.now()};
function save() { const tmp=DATA_FILE+'.tmp'; fs.writeFileSync(tmp, JSON.stringify(db,null,2)); fs.renameSync(tmp, DATA_FILE); }

function b64url(v){return Buffer.from(v).toString('base64url');}
function signJwt(payload){const h=b64url(JSON.stringify({alg:'HS256',typ:'JWT'}));const p=b64url(JSON.stringify(payload));const s=crypto.createHmac('sha256',JWT_SECRET).update(h+'.'+p).digest('base64url');return h+'.'+p+'.'+s;}
function verifyJwt(token){const [h,p,s]=String(token||'').split('.');if(!h||!p||!s)return null;const expected=crypto.createHmac('sha256',JWT_SECRET).update(h+'.'+p).digest('base64url');if(!crypto.timingSafeEqual(Buffer.from(s),Buffer.from(expected)))return null;const payload=JSON.parse(Buffer.from(p,'base64url').toString());if(payload.exp*1000<Date.now())return null;return payload;}
function hashPassword(password,salt){return crypto.scryptSync(password,salt,32).toString('hex');}
const salt=process.env.OWNER_SALT || crypto.randomBytes(16).toString('hex');
const ownerHash=process.env.OWNER_PASSWORD_HASH || hashPassword(OWNER_PASSWORD,salt);
let refreshTokens=new Map();
function issue(){const now=Math.floor(Date.now()/1000);const sessionId=crypto.randomUUID();const access=signJwt({sub:'owner-1',role:'OWNER',name:'Vakit+ Sahibi',sid:sessionId,exp:now+900});const refresh=crypto.randomBytes(48).toString('base64url');refreshTokens.set(refresh,{expires:now+60*60*24*30,sessionId});return {userId:'owner-1',displayName:'Vakit+ Sahibi',role:'OWNER',accessToken:access,refreshToken:refresh,expiresAtEpochSeconds:now+900};}
async function verifyAppCheck(req){
  if(process.env.REQUIRE_APP_CHECK !== 'true') return true;
  if(!firebaseAdmin) return false;
  const token=String(req.headers['x-firebase-appcheck']||'');
  if(!token) return false;
  try { await firebaseAdmin.appCheck().verifyToken(token); return true; } catch (_) { return false; }
}
function auth(req){const token=(req.headers.authorization||'').replace(/^Bearer\s+/i,'');const p=verifyJwt(token);if(!p||p.role!=='OWNER')return null;return p;}
function json(res,status,obj){const body=JSON.stringify(obj);res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(body);}
function readBody(req){return new Promise((resolve,reject)=>{let d='';req.on('data',c=>{d+=c;if(d.length>1000000)reject(new Error('body too large'));});req.on('end',()=>{try{resolve(d?JSON.parse(d):{})}catch(e){reject(e)}});req.on('error',reject);});}
function audit(action,details){db.audit.push({action,actor:'owner-1',timestamp:Date.now(),details});db.audit=db.audit.slice(-500);save();}


async function deliverNotification(campaign) {
  if(!firebaseAdmin || !FCM_PROJECT_ID) throw new Error('FCM_NOT_CONFIGURED');
  const target = campaign.topic || 'all';
  const messageId = await firebaseAdmin.messaging().send({topic:target, notification:{title:campaign.title,body:campaign.body}, data:{source:'vakitplus-owner', campaignId:String(campaign.id||'')}});
  db.notifications = db.notifications || [];
  db.notifications.push({id:campaign.id||crypto.randomUUID(), title:campaign.title, body:campaign.body, topic:target, messageId, timestamp:Date.now(), actor:'owner-1', status:'SENT'});
  db.notifications=db.notifications.slice(-500); save();
  audit('NOTIFICATION_SEND',`FCM konu: ${target}; ${campaign.title}`);
  return messageId;
}
async function processScheduledNotifications(){
  const now=Date.now();
  const due=(db.scheduledNotifications||[]).filter(x=>x.status==='SCHEDULED' && x.scheduledAtEpochMillis<=now);
  for(const item of due){
    try { item.status='SENDING'; save(); const id=await deliverNotification(item); item.status='SENT'; item.sentAtEpochMillis=Date.now(); item.messageId=id; }
    catch(e){ item.status='FAILED'; item.error=String(e.message||e); }
  }
  if(due.length) save();
}
setInterval(()=>{processScheduledNotifications().catch(()=>{});},30000).unref();

const server=http.createServer(async(req,res)=>{
  try {
    const url=new URL(req.url,`http://${req.headers.host}`);
    if(req.method==='GET'&&url.pathname==='/health') return json(res,200,{ok:true,service:'vakitplus-owner',time:Date.now()});
    if(req.method==='POST'&&url.pathname==='/v1/auth/login'){
      const ip=(req.headers['x-forwarded-for']||req.socket.remoteAddress||'unknown').toString().split(',')[0].trim();
      if(!rateLimitLogin(ip)) return json(res,429,{error:'TOO_MANY_LOGIN_ATTEMPTS',retryAfterSeconds:Math.ceil(RATE_WINDOW_MS/1000)});
      const b=await readBody(req); const id=String(b.identifier||'').trim().toLowerCase();
      const candidate=hashPassword(String(b.password||''),salt);
      if(id!==OWNER_EMAIL || !crypto.timingSafeEqual(Buffer.from(candidate,'hex'),Buffer.from(ownerHash,'hex'))) return json(res,401,{error:'INVALID_CREDENTIALS'});
      const session=issue(); audit('OWNER_LOGIN','Merkezi owner girişi'); return json(res,200,session);
    }
    if(req.method==='POST'&&url.pathname==='/v1/auth/refresh'){
      const b=await readBody(req);const item=refreshTokens.get(String(b.refreshToken||''));if(!item||item.expires<Date.now()/1000)return json(res,401,{error:'INVALID_REFRESH_TOKEN'});refreshTokens.delete(String(b.refreshToken));return json(res,200,issue());
    }
    if(req.method==='POST'&&url.pathname==='/v1/auth/logout'){const b=await readBody(req);refreshTokens.delete(String(b.refreshToken||''));return json(res,204,{});}
    if(!(await verifyAppCheck(req))) return json(res,403,{error:'APP_CHECK_FAILED'});
    const user=auth(req); if(!user) return json(res,401,{error:'UNAUTHORIZED'});
    if(req.method==='GET'&&url.pathname==='/v1/dashboard/summary'){
      const devices=db.devices||[];
      return json(res,200,{users:devices.length,devices:devices.length,activeDevices:devices.filter(d=>Date.now()-d.lastSeen<7*86400000).length,notificationsEnabled:devices.filter(d=>d.notificationsEnabled!==false).length,notificationsSent:(db.notifications||[]).filter(x=>x.status==='SENT').length,scheduledNotifications:(db.scheduledNotifications||[]).filter(x=>x.status==='SCHEDULED').length,drafts:(db.notificationDrafts||[]).length,auditEvents:(db.audit||[]).length,maintenance:!!db.system.maintenance});
    }
    if(req.method==='GET'&&url.pathname==='/v1/system/settings') return json(res,200,db.system);
    if(req.method==='PUT'&&url.pathname==='/v1/system/settings'){ const b=await readBody(req); db.system={maintenance:!!b.maintenance,announcement:String(b.announcement||'').trim(),updatedAt:Date.now()}; save(); audit('SYSTEM_SETTINGS_UPDATE',`Bakım: ${db.system.maintenance}`); return json(res,200,db.system); }
    if(req.method==='GET'&&url.pathname==='/v1/content') return json(res,200,db.content);
    if(req.method==='PUT'&&url.pathname==='/v1/content'){
      const b=await readBody(req);db.content={appSubtitle:String(b.appSubtitle||''),dailyMessage:String(b.dailyMessage||''),adminNote:String(b.adminNote||''),version:db.content.version+1};save();audit('CONTENT_UPDATE','Merkezi içerik güncellendi');return json(res,200,db.content);
    }
    if(req.method==='POST'&&url.pathname==='/v1/audit'){const b=await readBody(req);audit(String(b.action||'UNKNOWN'),String(b.details||''));return json(res,201,{ok:true});}
    if(req.method==='GET'&&url.pathname==='/v1/audit') return json(res,200,{events:db.audit.slice(-100).reverse()});
    if(req.method==='GET'&&url.pathname==='/v1/notifications/history') return json(res,200,{notifications:(db.notifications||[]).slice(-200).reverse(),scheduled:(db.scheduledNotifications||[]).slice(-100).reverse(),drafts:(db.notificationDrafts||[]).slice(-100).reverse()});
    if(req.method==='POST'&&url.pathname==='/v1/notifications/drafts'){ const b=await readBody(req); const item={id:crypto.randomUUID(),title:String(b.title||'').trim(),body:String(b.body||'').trim(),topic:String(b.topic||'all').trim(),createdAt:Date.now(),updatedAt:Date.now(),status:'DRAFT',actor:'owner-1'}; if(!item.title||!item.body)return json(res,400,{error:'TITLE_AND_BODY_REQUIRED'}); db.notificationDrafts=db.notificationDrafts||[]; db.notificationDrafts.push(item); db.notificationDrafts=db.notificationDrafts.slice(-200); save(); audit('NOTIFICATION_DRAFT_CREATE',item.title); return json(res,201,item); }
    if(req.method==='GET'&&url.pathname==='/v1/notifications/drafts') return json(res,200,{drafts:(db.notificationDrafts||[]).slice(-100).reverse()});
    if(req.method==='POST'&&url.pathname==='/v1/notifications/schedule'){ const b=await readBody(req); const t=Number(b.scheduledAtEpochMillis||0); const item={id:crypto.randomUUID(),title:String(b.title||'').trim(),body:String(b.body||'').trim(),topic:String(b.topic||'all').trim(),scheduledAtEpochMillis:t,status:'SCHEDULED',createdAt:Date.now(),actor:'owner-1'}; if(!item.title||!item.body||!t||t<Date.now()) return json(res,400,{error:'INVALID_SCHEDULE'}); db.scheduledNotifications=db.scheduledNotifications||[]; db.scheduledNotifications.push(item); db.scheduledNotifications=db.scheduledNotifications.slice(-200); save(); audit('NOTIFICATION_SCHEDULE',`${item.topic}; ${item.title}`); return json(res,201,item); }
    if(req.method==='DELETE'&&url.pathname.startsWith('/v1/notifications/schedule/')){ const id=decodeURIComponent(url.pathname.split('/').pop()); const item=(db.scheduledNotifications||[]).find(x=>x.id===id); if(!item)return json(res,404,{error:'NOT_FOUND'}); item.status='CANCELLED'; item.cancelledAtEpochMillis=Date.now(); save(); audit('NOTIFICATION_CANCEL',id); return json(res,200,{ok:true}); }
    if(req.method==='GET'&&url.pathname==='/v1/notifications/stats'){ const all=(db.notifications||[]); const sent=all.filter(x=>x.status==='SENT'); const byTopic={}; sent.forEach(x=>byTopic[x.topic]=(byTopic[x.topic]||0)+1); return json(res,200,{totalSent:sent.length,totalScheduled:(db.scheduledNotifications||[]).filter(x=>x.status==='SCHEDULED').length,totalDrafts:(db.notificationDrafts||[]).length,byTopic,lastSent:sent.slice(-10).reverse()}); }
    if(req.method==='GET'&&url.pathname==='/v1/devices'){ const devices=(db.devices||[]).slice().sort((a,b)=>b.lastSeen-a.lastSeen); return json(res,200,{devices,summary:{total:devices.length,active:devices.filter(d=>Date.now()-d.lastSeen<7*86400000).length,notificationsEnabled:devices.filter(d=>d.notificationsEnabled!==false).length}}); }
    if(req.method==='POST'&&url.pathname==='/v1/devices/register'){ const b=await readBody(req); const token=String(b.token||'').trim(); if(!token) return json(res,400,{error:'TOKEN_REQUIRED'}); const now=Date.now(); const existing=(db.devices||[]).find(d=>d.token===token); if(existing){Object.assign(existing,{platform:String(b.platform||'android'),appVersion:String(b.appVersion||''),city:String(b.city||''),notificationsEnabled:b.notificationsEnabled!==false,lastSeen:now});} else {(db.devices=db.devices||[]).push({id:crypto.randomUUID(),token,platform:'android',appVersion:String(b.appVersion||''),city:String(b.city||''),notificationsEnabled:b.notificationsEnabled!==false,createdAt:now,lastSeen:now});} save(); return json(res,200,{ok:true}); }
    if(req.method==='DELETE'&&url.pathname.startsWith('/v1/devices/')){ const id=decodeURIComponent(url.pathname.split('/').pop()); const before=(db.devices||[]).length; db.devices=(db.devices||[]).filter(d=>d.id!==id); save(); audit('DEVICE_REMOVE',`Cihaz kaldırıldı: ${id}`); return json(res,200,{ok:true,removed:before-db.devices.length}); }
    if(req.method==='POST'&&url.pathname==='/v1/notifications/send'){
      const b=await readBody(req); const title=String(b.title||'').trim(); const body=String(b.body||'').trim(); const topic=String(b.topic||'all').trim();
      if(!title||!body) return json(res,400,{error:'TITLE_AND_BODY_REQUIRED'});
      const scheduledAt=Number(b.scheduledAtEpochMillis||0);
      if(scheduledAt){ if(scheduledAt<Date.now()) return json(res,400,{error:'INVALID_SCHEDULE'}); const item={id:crypto.randomUUID(),title,body,topic,scheduledAtEpochMillis:scheduledAt,status:'SCHEDULED',createdAt:Date.now(),actor:'owner-1'}; db.scheduledNotifications=db.scheduledNotifications||[]; db.scheduledNotifications.push(item); save(); audit('NOTIFICATION_SCHEDULE',`${topic}; ${title}`); return json(res,202,{scheduled:true,id:item.id,target:`topic:${topic}`,scheduledAtEpochMillis:scheduledAt}); }
      try { const messageId=await deliverNotification({id:crypto.randomUUID(),title,body,topic}); return json(res,200,{messageId,target:`topic:${topic}`}); }
      catch(e){ return json(res,503,{error:'FCM_NOT_CONFIGURED',message:'FCM üretim yapılandırması gerekli.'}); }
    }
    return json(res,404,{error:'NOT_FOUND'});
  } catch(e){ console.error(e); return json(res,500,{error:'SERVER_ERROR'}); }
});
server.listen(PORT,HOST,()=>console.log(`Vakit+ Owner Backend listening on ${HOST}:${PORT}`));
