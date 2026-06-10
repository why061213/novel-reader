// 目录页面逻辑
const CatalogManager = {
    // 初始化
    init() {
        this.currentNovel = null;
        this.currentNovelId = null;
        this.chapters = [];
        this.filteredChapters = [];
        this.sortAsc = true;
        this.currentPage = 0;
        this.pageSize = 50;

        this.getNovelIdFromURL();
        this.setupEventListeners();

        if (this.currentNovelId) {
            this.loadNovelData();
        } else {
            this.showError('未找到小说ID');
        }
    },

    // 从URL获取小说ID
    getNovelIdFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        this.currentNovelId = urlParams.get('novel');

        if (!this.currentNovelId) {
            console.error('URL中未找到novel参数');
        }
    },

    // 设置事件监听器
    setupEventListeners() {
        // 返回书架按钮
        document.getElementById('backToLibraryBtn').addEventListener('click', () => {
            window.location.href = '../index.html';
        });

        // 开始阅读按钮
        document.getElementById('startReadingBtn').addEventListener('click', () => {
            this.startReading(0);
        });

        // 继续阅读按钮
        document.getElementById('continueReadingBtn').addEventListener('click', () => {
            this.continueReading();
        });

        // 删除小说按钮
        document.getElementById('deleteNovelBtn').addEventListener('click', () => {
            this.showDeleteConfirm();
        });

        // 确认删除按钮
        document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
            this.deleteNovel();
        });

        // 章节搜索
        const searchInput = document.getElementById('searchChaptersInput');
        const clearSearchBtn = document.getElementById('clearSearchBtn');

        searchInput.addEventListener('input', (e) => {
            this.searchChapters(e.target.value);
            clearSearchBtn.style.display = e.target.value ? 'block' : 'none';
        });

        clearSearchBtn.addEventListener('click', () => {
            searchInput.value = '';
            this.searchChapters('');
            clearSearchBtn.style.display = 'none';
        });

        // 排序按钮
        document.getElementById('sortAscBtn').addEventListener('click', () => {
            this.setSortOrder(true);
        });

        document.getElementById('sortDescBtn').addEventListener('click', () => {
            this.setSortOrder(false);
        });
    },

    // 加载小说数据
    async loadNovelData() {
        try {
            // 显示加载状态
            this.showLoading();

            // 并行加载小说基本信息和章节列表
            await Promise.all([
                this.loadNovelInfo(),
                this.loadChapters()
            ]);

            // 渲染数据
            this.renderNovelInfo();
            this.renderChapters();

            // 显示内容
            this.showContent();

        } catch (error) {
            console.error('加载小说数据失败:', error);
            this.showError('无法加载小说信息，请返回书架重试');
        }
    },

    // 加载小说基本信息
    async loadNovelInfo() {
        try {
            const response = await fetch(`/api/novels/${this.currentNovelId}`);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const novel = await response.json();
            this.currentNovel = this.adaptNovelFields(novel);

        } catch (error) {
            console.error('加载小说基本信息失败:', error);

            // 尝试从本地存储加载
            try {
                const novels = StorageManager.getNovels() || [];
                const novel = novels.find(n => n.id === this.currentNovelId);

                if (novel) {
                    this.currentNovel = this.adaptNovelFields(novel);
                } else {
                    throw new Error('小说不存在');
                }
            } catch (localError) {
                console.error('从本地存储加载失败:', localError);
                throw error;
            }
        }
    },

    // 加载章节列表
    async loadChapters() {
        try {
            // 使用新的API获取章节列表
            const response = await fetch(`/api/novels/${this.currentNovelId}/chapters`);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const chaptersData = await response.json();

            // 转换API返回的数据格式
            this.chapters = chaptersData.map(chapter => ({
                id: chapter.id,
                title: chapter.title,
                isRead: chapter.isRead || false,
                novelId: chapter.novelId,
                novelName: chapter.novelName
            }));

            this.filteredChapters = [...this.chapters];

        } catch (error) {
            console.error('加载章节列表失败:', error);

            // 如果API失败，尝试从小说数据中解析章节
            if (this.currentNovel) {
                this.chapters = this.parseChaptersFromNovel(this.currentNovel);
                this.filteredChapters = [...this.chapters];
            } else {
                this.chapters = [];
                this.filteredChapters = [];
            }
        }
    },

    // 从小说数据中解析章节（备用方案）
    parseChaptersFromNovel(novel) {
        const chapters = novel.chapter || novel.chapters || [];
        const history = novel.history || novel.lastReadChapter || 0;

        return chapters.map((chapter, index) => {
            let chapterTitle = chapter;
            if (typeof chapter !== 'string') {
                chapterTitle = `第${index + 1}章`;
            }

            const isRead = index < history;

            return {
                id: index,
                title: chapterTitle,
                isRead: isRead,
                novelId: novel.id,
                novelName: novel.name || novel.title || '未知小说'
            };
        });
    },

    // 字段适配器
    adaptNovelFields(novel) {
        return {
            ...novel,
            name: novel.name || novel.title || '未知小说',
            chapter: novel.chapter || novel.chapters || [],
            history: novel.history || novel.lastReadChapter || 0,
            totalChapters: novel.totalChapters || (novel.chapter ? novel.chapter.length : 0) ||
                (novel.chapters ? novel.chapters.length : 0),
            lastRead: novel.lastRead !== undefined ? novel.lastRead : (novel.history || 0),
        };
    },

    // 渲染小说信息
    // 渲染小说信息
    renderNovelInfo() {
        if (!this.currentNovel) return;

        const novelName = this.currentNovel.name || this.currentNovel.title || '未知小说';
        const totalChapters = this.currentNovel.totalChapters || this.chapters.length;
        const readChapters = this.currentNovel.history || 0;
        const progressPercent = totalChapters > 0 ? Math.round((readChapters / totalChapters) * 100) : 0;
        const totalWords = this.currentNovel.totalWords || '未知';
        const lastRead = this.currentNovel.lastRead || 0;
        const lastReadChapter = this.chapters[lastRead];
        const lastReadText = lastReadChapter ? `第${lastRead + 1}章` : '无';

        document.getElementById('lastReadStat').textContent = lastReadText;

        // 更新页面标题
        document.title = `${novelName} - 目录`;

        // 更新小说信息
        document.getElementById('novelTitle').textContent = novelName;
        document.getElementById('novelId').textContent = this.currentNovelId;

        // 更新作者信息（如果有）
        if (this.currentNovel.author) {
            document.getElementById('novelAuthor').textContent = this.currentNovel.author;
        } else {
            document.getElementById('novelAuthor').textContent = '未知';
        }

        // 更新统计信息
        document.getElementById('totalChaptersStat').textContent = totalChapters;
        document.getElementById('readChaptersStat').textContent = readChapters;
        document.getElementById('readingProgressStat').textContent = `${progressPercent}%`;
        //document.getElementById('totalWordsStat').textContent = this.formatNumber(totalWords);

        // 更新删除确认框中的小说名称
        document.getElementById('deleteNovelName').textContent = novelName;

        // 更新章节计数
        // 绑定“上次阅读”统计块的点击跳转
        const lastReadStatEl = document.getElementById('lastReadStat');
        if (lastReadStatEl) {
            lastReadStatEl.style.cursor = 'pointer';
            lastReadStatEl.title = '点击跳转到上次阅读的章节';
            lastReadStatEl.onclick = () => {
                const lastRead = this.currentNovel?.lastRead;
                if (lastRead !== undefined && lastRead !== null) {
                    this.startReading(lastRead);
                }
            };
        }
        this.updateChaptersCount();
    },

    // 更新章节计数显示
    updateChaptersCount() {
        const countElement = document.getElementById('chaptersCount');
        const displayRangeElement = document.getElementById('currentDisplayRange');

        const total = this.chapters.length;
        const displayed = this.filteredChapters.length;

        if (displayed === total) {
            countElement.textContent = `共 ${total} 章`;
        } else {
            countElement.textContent = `共 ${total} 章 (搜索到 ${displayed} 章)`;
        }

        if (displayed > 0) {
            displayRangeElement.textContent = `1-${displayed}`;
        } else {
            displayRangeElement.textContent = '0-0';
        }
    },

    // 渲染章节列表
    renderChapters() {
        const chaptersList = document.getElementById('chaptersList');
        const catalogEmptyState = document.getElementById('catalogEmptyState');
        const searchEmptyState = document.getElementById('searchEmptyState');


        if (this.filteredChapters.length === 0) {
            chaptersList.style.display = 'none';

            if (this.chapters.length === 0) {
                catalogEmptyState.style.display = 'flex';
                searchEmptyState.style.display = 'none';
            } else {
                catalogEmptyState.style.display = 'none';
                searchEmptyState.style.display = 'flex';
            }
            return;
        }

        chaptersList.style.display = 'block';
        catalogEmptyState.style.display = 'none';
        searchEmptyState.style.display = 'none';

        // 排序章节
        let sortedChapters = [...this.filteredChapters];
        if (!this.sortAsc) {
            sortedChapters.reverse();
        }

        // 生成章节HTML
        chaptersList.innerHTML = sortedChapters.map(chapter => {
            const chapterNumber = chapter.id + 1;

            const isMaxHistory = chapter.id === (this.currentNovel?.history || 0);
            const isLastRead = chapter.id === (this.currentNovel?.lastRead || 0);

            let chapterClass = 'chapter-item';
            if (isMaxHistory) chapterClass += ' max-progress';
            if (isLastRead) chapterClass += ' last-read';

            let statusText = '未读';
            if (isMaxHistory && isLastRead) {
                statusText = '已读至 · 上次';
            } else if (isMaxHistory) {
                statusText = '已读至';
            } else if (isLastRead) {
                statusText = '上次阅读';
            } else if (chapter.isRead) {
                statusText = '已读';
            }

            return `
        <div class="${chapterClass}" data-chapter-id="${chapter.id}">
            <div class="chapter-number">${chapterNumber}</div>
            <div class="chapter-info">
                <div class="chapter-title" title="${chapter.title}">${chapter.title}</div>
                <div class="chapter-progress">
                    ${statusText}
                </div>
            </div>
            <div class="chapter-actions">
                <button class="btn btn-sm btn-outline-primary read-chapter-btn" 
                        data-chapter-id="${chapter.id}">
                    <i class="fas fa-book-open"></i>
                </button>
            </div>
        </div>
    `;
        }).join('');

        // 添加章节点击事件
        chaptersList.querySelectorAll('.chapter-item').forEach(item => {
            const chapterId = parseInt(item.dataset.chapterId);

            item.addEventListener('click', (e) => {
                if (!e.target.closest('.read-chapter-btn')) {
                    this.startReading(chapterId);
                }
            });
        });

        // 添加阅读按钮点击事件
        chaptersList.querySelectorAll('.read-chapter-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const chapterId = parseInt(btn.dataset.chapterId);
                this.startReading(chapterId);
            });
        });

        // 更新章节计数
        this.updateChaptersCount();

    },

    // 设置排序顺序
    setSortOrder(ascending) {
        this.sortAsc = ascending;

        const ascBtn = document.getElementById('sortAscBtn');
        const descBtn = document.getElementById('sortDescBtn');

        if (ascending) {
            ascBtn.classList.add('active');
            descBtn.classList.remove('active');
        } else {
            ascBtn.classList.remove('active');
            descBtn.classList.add('active');
        }

        this.renderChapters();
    },

    // 搜索章节
    searchChapters(keyword) {
        if (!keyword.trim()) {
            this.filteredChapters = [...this.chapters];
        } else {
            const searchTerm = keyword.toLowerCase();
            this.filteredChapters = this.chapters.filter(chapter =>
                chapter.title.toLowerCase().includes(searchTerm) ||
                chapter.id.toString().includes(searchTerm)
            );
        }

        this.renderChapters();
    },

    // 开始阅读
    startReading(chapterId) {
        if (!this.currentNovelId) return;

        // 更新阅读进度（如果跳转到新章节）
        if (this.currentNovel && chapterId > (this.currentNovel.history || 0)) {
            this.updateReadingProgress(chapterId);
        }

        // 跳转到阅读页面
        window.location.href = `../reading.html?novelId=${this.currentNovelId}&chapter=${chapterId}`;
    },

    // 继续阅读
    continueReading() {
        if (!this.currentNovel) return;
        const historyChapter = this.currentNovel.history || 0;
        this.startReading(historyChapter);
    },

    // 更新阅读进度
    async updateReadingProgress(chapterId) {
        try {
            // 发送到后端更新进度
            const params = new URLSearchParams();
            params.append('chapterNumber', chapterId);

            const response = await fetch(`/api/novels/${this.currentNovelId}/progress`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params.toString()
            });

            if (response.ok) {
                console.log('阅读进度更新成功');

                // 更新本地数据
                if (this.currentNovel) {
                    if (this.currentNovel.history < chapterId) {
                        this.currentNovel.history = chapterId;
                    }

                    // 重新渲染
                    this.renderNovelInfo();
                    this.renderChapters();
                }
            }
        } catch (error) {
            console.error('更新阅读进度失败:', error);
        }
    },

    // 显示删除确认框
    showDeleteConfirm() {
        if (!this.currentNovel) return;

        const modal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
        modal.show();
    },

    // 删除小说
    async deleteNovel() {
        try {
            // 关闭模态框
            const modal = bootstrap.Modal.getInstance(document.getElementById('deleteConfirmModal'));
            modal.hide();

            // 创建删除成功覆盖层
            this.createDeleteSuccessOverlay();

        } catch (error) {
            console.error('删除小说失败:', error);

            // 恢复显示
            this.showContent();

            // 显示错误提示
            alert('删除失败: ' + error.message);
        }
    },

// 创建删除成功覆盖层
    createDeleteSuccessOverlay() {
        // 创建覆盖层容器
        const overlay = document.createElement('div');
        overlay.className = 'delete-success-container';
        overlay.id = 'deleteSuccessOverlay';

        // 创建删除成功内容
        overlay.innerHTML = `
        <div class="delete-success-content">
            <div class="delete-success-icon">
                <i class="fas fa-check-circle"></i>
            </div>
            <h3 class="delete-success-title">删除成功</h3>
            <p class="delete-success-message">小说已成功删除</p>
            <p class="delete-success-timer">3秒后自动返回书架...</p>
        </div>
    `;

        // 添加到页面
        document.getElementById('mainContent').appendChild(overlay);

        // 发送删除请求
        this.performNovelDeletion();
    },

// 执行小说删除操作
    async performNovelDeletion() {
        try {
            // 发送删除请求到后端
            const response = await fetch(`/api/novels/${this.currentNovelId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error(`删除失败: ${response.status}`);
            }

            const result = await response.json();

            if (!result.success) {
                throw new Error(result.message || '删除失败');
            }

            // 3秒后自动跳转
            let countdown = 3;
            const timerElement = document.querySelector('.delete-success-timer');

            const countdownInterval = setInterval(() => {
                countdown--;
                if (timerElement) {
                    timerElement.textContent = `${countdown}秒后自动返回书架...`;
                }

                if (countdown <= 0) {
                    clearInterval(countdownInterval);
                    window.location.href = '../index.html';
                }
            }, 1000);

        } catch (error) {
            console.error('执行删除操作失败:', error);

            // 移除覆盖层
            const overlay = document.getElementById('deleteSuccessOverlay');
            if (overlay) {
                overlay.remove();
            }

            // 显示错误提示
            alert('删除失败: ' + error.message);
        }
    },

// 显示加载状态（更新）
    showLoading() {
        document.getElementById('loadingState').style.display = 'flex';
        document.getElementById('errorState').style.display = 'none';
        document.getElementById('novelHeader').style.display = 'none';
        document.getElementById('catalogSection').style.display = 'none';

        // 确保删除成功覆盖层被移除（如果有）
        const overlay = document.getElementById('deleteSuccessOverlay');
        if (overlay) {
            overlay.remove();
        }
    },

// 显示内容（更新）
    showContent() {
        document.getElementById('loadingState').style.display = 'none';
        document.getElementById('errorState').style.display = 'none';
        document.getElementById('novelHeader').style.display = 'block';
        document.getElementById('catalogSection').style.display = 'block';

        // 确保删除成功覆盖层被移除（如果有）
        const overlay = document.getElementById('deleteSuccessOverlay');
        if (overlay) {
            overlay.remove();
        }

        // 如果没有章节，显示空状态
        if (this.chapters.length === 0) {
            document.getElementById('catalogEmptyState').style.display = 'flex';
            document.getElementById('chaptersList').style.display = 'none';
        }
    },

// 显示错误（更新）
    showError(message) {
        document.getElementById('loadingState').style.display = 'none';
        document.getElementById('novelHeader').style.display = 'none';
        document.getElementById('catalogSection').style.display = 'none';

        const errorState = document.getElementById('errorState');
        errorState.style.display = 'block';
        document.getElementById('errorMessage').textContent = message;

        // 确保删除成功覆盖层被移除（如果有）
        const overlay = document.getElementById('deleteSuccessOverlay');
        if (overlay) {
            overlay.remove();
        }
    },

    // 格式化数字
    formatNumber(num) {
        if (typeof num !== 'number') return num;

        if (num >= 10000) {
            return (num / 10000).toFixed(1) + '万';
        } else if (num >= 1000) {
            return (num / 1000).toFixed(1) + '千';
        }
        return num.toString();
    }
};

// 初始化目录页面
document.addEventListener('DOMContentLoaded', () => {
    CatalogManager.init();
});

// 导出为全局对象
window.CatalogManager = CatalogManager;