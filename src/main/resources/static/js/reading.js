// reading.js - 单章阅读控制逻辑
(function () {
    // 从 URL 获取参数
    const params = new URLSearchParams(window.location.search);
    const novelId = params.get('novelId');
    let currentChapter = parseInt(params.get('chapter') || 0);
    let totalChapters = 0;          // 总章节数，首次加载后获取
    let novelName = '未知小说';

    // DOM 元素
    const chapterTitleEl = document.getElementById('chapterTitle');
    const chapterContentEl = document.getElementById('chapterContent');
    const novelNameEl = document.getElementById('novelName');
    const chapterIndicator = document.getElementById('chapterIndicator');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    // 显示加载状态
    function showLoading() {
        chapterContentEl.innerHTML = '<div class="text-center mt-5"><i class="fas fa-spinner fa-spin"></i> 加载中...</div>';
    }

    // 加载章节内容
    async function loadChapter(chapterNumber) {
        showLoading();
        try {
            const response = await fetch(`/api/novels/${novelId}/chapters/${chapterNumber}`);
            const data = await response.json();
            if (!data.success) {
                chapterContentEl.innerHTML = `<div class="alert alert-danger">${data.message || '章节加载失败'}</div>`;
                return;
            }

            // 更新标题
            chapterTitleEl.textContent = data.title || `第 ${chapterNumber + 1} 章`;
            // 内容直接插入（保留换行）
            chapterContentEl.textContent = data.content || '';

            // 更新小说名（第一次加载时设置）
            if (!novelName || novelName === '未知小说') {
                novelName = data.novelName || '未知小说';
                novelNameEl.textContent = novelName;
            }

            // 保存总章节数（第一次加载时获取，或从章节列表接口获取更可靠）
            if (!totalChapters && data.totalChapters) {
                totalChapters = data.totalChapters;
            }

            // 更新导航按钮状态
            currentChapter = chapterNumber;
            updateNavigation(data);

            // 自动保存阅读进度
            saveProgress(chapterNumber);

        } catch (error) {
            console.error('加载章节失败:', error);
            chapterContentEl.innerHTML = '<div class="alert alert-danger">网络错误，请重试</div>';
        }
    }

    // 更新导航按钮及进度显示
    function updateNavigation(data) {
        const prev = data.prevChapter;
        const next = data.nextChapter;
        prevBtn.disabled = (prev === -1 || prev === null);
        nextBtn.disabled = (next === -1 || next === null);

        // 如果尚未获得总章节数，从 data.totalChapters 获取
        if (data.totalChapters) totalChapters = data.totalChapters;
        chapterIndicator.textContent = totalChapters > 0 ? `${currentChapter + 1} / ${totalChapters}` : '';
    }

    // 保存阅读进度
    async function saveProgress(chapter) {
        try {
            await fetch(`/api/novels/${novelId}/progress?chapterNumber=${chapter}`, {
                method: 'POST'
            });
        } catch (e) {
            console.warn('进度保存失败:', e);
        }
    }

    // 翻页事件
    function goToChapter(delta) {
        const next = currentChapter + delta;
        if (next < 0 || (totalChapters > 0 && next >= totalChapters)) return;
        // 更新 URL 但不刷新页面（方便刷新或分享）
        const newUrl = `?novelId=${novelId}&chapter=${next}`;
        history.pushState({chapter: next}, '', newUrl);
        loadChapter(next);
    }

    // 键盘监听
    document.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
            e.preventDefault();
            if (!prevBtn.disabled) goToChapter(-1);
        } else if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
            e.preventDefault();
            if (!nextBtn.disabled) goToChapter(1);
        }
    });

    // 按钮绑定
    prevBtn.addEventListener('click', () => goToChapter(-1));
    nextBtn.addEventListener('click', () => goToChapter(1));

    // 触摸滑动支持（移动端）
    let touchStartX = 0;
    document.addEventListener('touchstart', (e) => {
        touchStartX = e.changedTouches[0].screenX;
    });
    document.addEventListener('touchend', (e) => {
        const deltaX = e.changedTouches[0].screenX - touchStartX;
        if (Math.abs(deltaX) < 50) return;
        if (deltaX > 0 && !prevBtn.disabled) goToChapter(-1);  // 右滑上一章
        else if (deltaX < 0 && !nextBtn.disabled) goToChapter(1); // 左滑下一章
    });

    // 初始加载
    if (!novelId) {
        chapterContentEl.innerHTML = '<div class="alert alert-warning">缺少小说 ID 参数，请从书架进入</div>';
    } else {
        loadChapter(currentChapter);
        // 额外获取章节列表，以确保 totalChapters 准确
        fetch(`/api/novels/${novelId}/chapters`)
            .then(res => res.json())
            .then(list => {
                if (Array.isArray(list) && list.length > 0) {
                    totalChapters = list.length;
                    chapterIndicator.textContent = `${currentChapter + 1} / ${totalChapters}`;
                }
            })
            .catch(() => {});
    }
    // 返回主页
    document.getElementById('backHomeBtn').addEventListener('click', () => {
        window.location.href = '../index.html';  // 根据实际路径调整
    });

    // 返回目录
    document.getElementById('backCatalogBtn').addEventListener('click', () => {
        if (novelId) {
            window.location.href = `../catalog.html?novel=${novelId}`;  // 参数改为 novel 对齐目录页
        } else {
            window.location.href = '../index.html';
        }
    });
})();
