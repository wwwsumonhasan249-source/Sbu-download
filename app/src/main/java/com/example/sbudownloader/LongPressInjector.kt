package com.example.sbudownloader

object LongPressInjector {
    val JS = """
    (function() {
        if (window.__lpInstalled) return;
        window.__lpInstalled = true;

        function absUrl(u) {
            try { return new URL(u, location.href).href; } catch(e) { return u || ''; }
        }

        function getVideoSources(v) {
            const list = [];
            const seen = new Set();
            function add(url, type) {
                if (!url) return;
                const abs = absUrl(url);
                if (!abs || seen.has(abs)) return;
                seen.add(abs);
                list.push({ url: abs, type: type || '' });
            }
            if (v.currentSrc) add(v.currentSrc, '');
            if (v.src) add(v.src, '');
            const srcs = v.getElementsByTagName('source');
            for (let i = 0; i < srcs.length; i++) add(srcs[i].src, srcs[i].type || '');
            return list;
        }

        function getImageSrc(img) {
            if (img.currentSrc) return absUrl(img.currentSrc);
            if (img.src) return absUrl(img.src);
            return '';
        }

        function removeMenu() {
            const old = document.getElementById('__lp_menu__');
            if (old) old.remove();
        }

        function showMenu(x, y, items) {
            removeMenu();
            const menu = document.createElement('div');
            menu.id = '__lp_menu__';
            menu.style.cssText = 'position:fixed;top:'+y+'px;left:'+x+'px;min-width:220px;background:#fff;border-radius:12px;box-shadow:0 8px 24px rgba(0,0,0,0.3);z-index:2147483647;overflow:hidden;font-family:sans-serif;font-size:15px;';
            items.forEach(function(item) {
                const row = document.createElement('div');
                row.style.cssText = 'padding:14px 18px;cursor:pointer;color:' + (item.color||'#222') + ';display:flex;align-items:center;gap:10px;font-weight:500;';
                row.innerHTML = '<span style="font-size:18px">'+item.icon+'</span><span>'+item.label+'</span>';
                row.onclick = function(e){ e.preventDefault(); e.stopPropagation(); removeMenu(); item.action(); };
                menu.appendChild(row);
            });
            document.body.appendChild(menu);
            const r = menu.getBoundingClientRect();
            if (r.right > window.innerWidth) menu.style.left = (window.innerWidth - r.width - 10) + 'px';
            if (r.bottom > window.innerHeight) menu.style.top = (window.innerHeight - r.height - 10) + 'px';
            setTimeout(function(){ document.addEventListener('click', removeMenu, {once:true}); }, 100);
        }

        let timer = null;
        document.addEventListener('touchstart', function(e) {
            const t = e.touches[0];
            timer = setTimeout(function() {
                let el = document.elementFromPoint(t.clientX, t.clientY);
                if (!el) return;
                let tag = el.tagName.toLowerCase();
                
                if (tag === 'img' && window.AndroidContextMenu) {
                    showMenu(t.clientX, t.clientY, [
                        { icon: '⬇️', label: 'ছবি ডাউনলোড', color: '#D32F2F', action: function() { window.AndroidContextMenu.requestDownload(getImageSrc(el), 'image/jpeg', 'image_'+Date.now()+'.jpg'); } },
                        { icon: '📋', label: 'লিংক কপি', color: '#1976D2', action: function() { window.AndroidContextMenu.requestCopy(getImageSrc(el)); } }
                    ]);
                } 
                else if (tag === 'video' && window.AndroidContextMenu) {
                    let srcs = getVideoSources(el);
                    if (srcs.length > 0) {
                        showMenu(t.clientX, t.clientY, [
                            { icon: '⬇️', label: 'ভিডিও ডাউনলোড', color: '#D32F2F', action: function() { window.AndroidContextMenu.requestDownload(srcs[0].url, 'video/mp4', 'video_'+Date.now()+'.mp4'); } },
                            { icon: '📋', label: 'লিংক কপি', color: '#1976D2', action: function() { window.AndroidContextMenu.requestCopy(srcs[0].url); } }
                        ]);
                    }
                }
            }, 600);
        }, {passive:true});

        document.addEventListener('touchend', function() { clearTimeout(timer); }, true);
        document.addEventListener('touchmove', function() { clearTimeout(timer); }, {passive:true});
        document.addEventListener('contextmenu', function(e){ e.preventDefault(); }, true);
    })();
    """.trimIndent()
}