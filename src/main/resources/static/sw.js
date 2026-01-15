const CACHE_NAME = 'sib-portal-v1';
const STATIC_CACHE = 'sib-static-v1';
const DYNAMIC_CACHE = 'sib-dynamic-v1';

// Assets to cache on install
const STATIC_ASSETS = [
    '/',
    '/css/style.css',
    '/css/responsive.css',
    '/css/skeleton.css',
    '/css/breadcrumb.css',
    '/images/logo.png',
    '/images/hero.png',
    '/manifest.json',
    'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap'
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
    console.log('[Service Worker] Installing...');
    event.waitUntil(
        caches.open(STATIC_CACHE)
            .then((cache) => {
                console.log('[Service Worker] Caching static assets');
                return cache.addAll(STATIC_ASSETS);
            })
            .catch((error) => {
                console.error('[Service Worker] Cache failed:', error);
            })
    );
    self.skipWaiting();
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
    console.log('[Service Worker] Activating...');
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames
                    .filter((name) => name !== STATIC_CACHE && name !== DYNAMIC_CACHE)
                    .map((name) => {
                        console.log('[Service Worker] Deleting old cache:', name);
                        return caches.delete(name);
                    })
            );
        })
    );
    return self.clients.claim();
});

// Fetch event - serve from cache, fallback to network
self.addEventListener('fetch', (event) => {
    const { request } = event;
    const url = new URL(request.url);

    // Skip non-GET requests
    if (request.method !== 'GET') {
        return;
    }

    // Skip Chrome extensions and other protocols
    if (!url.protocol.startsWith('http')) {
        return;
    }

    // Strategy: Cache-first for static assets, Network-first for API calls
    if (isStaticAsset(url)) {
        event.respondWith(cacheFirst(request));
    } else if (isApiCall(url)) {
        event.respondWith(networkFirst(request));
    } else {
        event.respondWith(networkFirst(request));
    }
});

// Cache-first strategy
async function cacheFirst(request) {
    const cache = await caches.open(STATIC_CACHE);
    const cached = await cache.match(request);

    if (cached) {
        return cached;
    }

    try {
        const response = await fetch(request);
        if (response.ok) {
            cache.put(request, response.clone());
        }
        return response;
    } catch (error) {
        console.error('[Service Worker] Fetch failed:', error);
        return new Response('Offline - Asset not available', {
            status: 503,
            statusText: 'Service Unavailable'
        });
    }
}

// Network-first strategy
async function networkFirst(request) {
    const cache = await caches.open(DYNAMIC_CACHE);

    try {
        const response = await fetch(request);
        if (response.ok) {
            cache.put(request, response.clone());
        }
        return response;
    } catch (error) {
        console.error('[Service Worker] Network failed, trying cache:', error);
        const cached = await cache.match(request);

        if (cached) {
            return cached;
        }

        // Return offline page for navigation requests
        if (request.mode === 'navigate') {
            const offlinePage = await cache.match('/');
            if (offlinePage) {
                return offlinePage;
            }
        }

        return new Response('Offline - Please check your connection', {
            status: 503,
            statusText: 'Service Unavailable'
        });
    }
}

// Helper: Check if request is for static asset
function isStaticAsset(url) {
    return url.pathname.match(/\.(css|js|png|jpg|jpeg|svg|gif|webp|woff|woff2|ttf|eot)$/);
}

// Helper: Check if request is API call
function isApiCall(url) {
    return url.pathname.startsWith('/api/') ||
        url.pathname.includes('/validate-otp') ||
        url.pathname.includes('/resend-otp');
}

// Handle messages from clients
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});
