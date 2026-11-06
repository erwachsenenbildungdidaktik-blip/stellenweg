// Offline-Cache: App einmal mit Internet öffnen, danach läuft sie ohne.
// Copyright (c) 2026 Hugo Rohrbacher. Alle Rechte vorbehalten.
const CACHE = 'stellenweg-v6';
const FILES = ['./', 'index.html', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png',
  'vendor/pdf.min.js', 'vendor/pdf.worker.min.js', 'vendor/pdf-lib.min.js', 'vendor/jszip.min.js'];
self.addEventListener('install', e => e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES)).then(() => self.skipWaiting())));
self.addEventListener('activate', e => e.waitUntil(
  caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())));
// Netz zuerst (damit Updates ankommen), sonst Cache
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  e.respondWith(fetch(e.request).then(r => {
    const copy = r.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); return r;
  }).catch(() => caches.match(e.request, { ignoreSearch: true })));
});

// Erinnerungen als Mitteilung, ohne Server: Die App legt die Liste in IndexedDB ab (kv 'notify'),
// hier werden fällige Einträge angezeigt und in kv 'notified' gemerkt, damit nichts doppelt kommt.
function kv(mode, fn) {
  return new Promise(res => {
    const r = indexedDB.open('stellenweg', 1);
    r.onupgradeneeded = () => { r.result.createObjectStore('kv'); r.result.createObjectStore('blobs'); };
    r.onerror = () => res(null);
    r.onsuccess = () => {
      const db = r.result;
      try {
        const tx = db.transaction('kv', mode), q = fn(tx.objectStore('kv'));
        tx.oncomplete = () => { db.close(); res(q && q.result); };
        tx.onerror = () => { db.close(); res(null); };
      } catch (e) { db.close(); res(null); }
    };
  });
}
async function checkReminders() {
  const n = await kv('readonly', s => s.get('notify'));
  if (!n || !n.on || !Array.isArray(n.items)) return;
  const seen = (await kv('readonly', s => s.get('notified'))) || {}, now = Date.now();
  let changed = false;
  for (const it of n.items) {
    if (it.at > now || seen[it.key]) continue;
    seen[it.key] = now; changed = true;
    await self.registration.showNotification(it.title, { body: it.body, tag: it.key, icon: 'icon-192.png', badge: 'icon-192.png', data: { go: it.go } });
  }
  for (const k in seen) if (now - seen[k] > 60 * 864e5) { delete seen[k]; changed = true; }
  if (changed) await kv('readwrite', s => s.put(seen, 'notified'));
}
self.addEventListener('periodicsync', e => { if (e.tag === 'stellenweg-erinnerungen') e.waitUntil(checkReminders()); });
self.addEventListener('message', e => { if (e.data && e.data.type === 'check') e.waitUntil(checkReminders()); });
// Tipp auf die Mitteilung öffnet die App an der richtigen Stelle
self.addEventListener('notificationclick', e => {
  e.notification.close();
  const go = (e.notification.data && e.notification.data.go) || 'heute';
  e.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(L => {
    if (L.length) { L[0].postMessage({ type: 'go', go }); return L[0].focus(); }
    return self.clients.openWindow('./?go=' + encodeURIComponent(go));
  }));
});
