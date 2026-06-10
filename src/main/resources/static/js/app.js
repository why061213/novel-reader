// 主应用逻辑
const AppManager = {
    // 初始化
    init() {
        this.novels = [];
        this.filteredNovels = [];
        this.history = [];

        this.setupEventListeners();
        this.loadNovels();
        this.loadHistory();
        this.updateStats();
    },

    // 设置事件监听器
    setupEventListeners() {
        // 导入小说按钮
        document.getElementById('addNovelBtn').addEventListener('click', () => {
            window.location.href = 'import.html';
        });

        // 搜索功能
        const searchInput = document.getElementById('searchInput');
        const clearSearchBtn = document.getElementById('clearSearchBtn');

        searchInput.addEventListener('input', (e) => {
            this.searchNovels(e.target.value);
            clearSearchBtn.style.display = e.target.value ? 'block' : 'none';
        });

        clearSearchBtn.addEventListener('click', () => {
            searchInput.value = '';
            this.searchNovels('');
            clearSearchBtn.style.display = 'none';
        });

        // 清空历史记录按钮
        document.getElementById('clearHistoryBtn').addEventListener('click', (e) => {
            e.stopPropagation();
            if (confirm('确定要清空所有阅读历史记录吗？')) {
                this.clearHistory();
            }
        });

        // 侧边栏导航
        const navLinks = document.querySelectorAll('.sidebar-nav .nav-link');
        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                if (link.getAttribute('href') !== 'index.html') {
                    e.preventDefault();
                    window.location.href = link.getAttribute('href');
                }
            });
        });
    },

    // 加载小说列表
    // 修改 loadNovels 方法
    loadNovels() {
        try {
            // 从后端API获取小说列表
            fetch('/api/novels')
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(novels => {
                    console.log('从后端获取到小说列表:', novels);
                    this.novels = novels;
                    this.filteredNovels = [...this.novels];
                    this.renderNovels();
                    this.updateStats();

                    // 如果有小说，隐藏空状态
                    if (this.novels.length > 0) {
                        document.getElementById('emptyState').style.display = 'none';
                    }
                })
                .catch(error => {
                    console.error('从后端加载小说失败:', error);

                    // 如果后端失败，尝试从本地存储加载
                    this.novels = StorageManager.getNovels() || [];
                    this.filteredNovels = [...this.novels];
                    this.renderNovels();
                    this.updateStats();
                });

        } catch (error) {
            console.error('加载小说失败:', error);
            this.novels = [];
            this.filteredNovels = [];
            this.renderNovels();
            this.updateStats();
        }
    },

    // 搜索小说
    searchNovels(keyword) {
        if (!keyword.trim()) {
            this.filteredNovels = [...this.novels];
        } else {
            const searchTerm = keyword.toLowerCase();
            this.filteredNovels = this.novels.filter(novel =>
                novel.name.toLowerCase().includes(searchTerm) ||
                (novel.author && novel.author.toLowerCase().includes(searchTerm))
            );
        }

        this.renderNovels();
    },

    // 渲染小说列表
    renderNovels() {
        const container = document.getElementById('novelListContainer');
        const emptyState = document.getElementById('emptyState');
        const searchEmptyState = document.getElementById('searchEmptyState');

        // 清空容器
        container.innerHTML = '';

        if (this.filteredNovels.length === 0) {
            // 显示空状态
            if (this.novels.length === 0) {
                emptyState.style.display = 'block';
                searchEmptyState.style.display = 'none';
            } else {
                emptyState.style.display = 'none';
                searchEmptyState.style.display = 'block';
            }
            return;
        }

        // 隐藏空状态
        emptyState.style.display = 'none';
        searchEmptyState.style.display = 'none';

        // 渲染小说卡片
        this.filteredNovels.forEach(novel => {
            const novelCard = this.createNovelCard(novel);
            container.appendChild(novelCard);
        });
    },

    // 创建小说卡片
    createNovelCard(novel) {
        const col = document.createElement('div');
        col.className = 'col-xl-3 col-lg-4 col-md-6 col-sm-12';

        // 计算阅读进度
        const progressPercent = novel.history && novel.chapter ?
            Math.round((novel.history + 1) / novel.chapter.length * 100) : 0;

        // 获取最后阅读章节标题
        const lastChapterTitle = novel.chapter && novel.chapter[novel.history] ?
            novel.chapter[novel.history] : '未开始阅读';

        col.innerHTML = `
            <div class="novel-card" data-novel-id="${novel.id}">
                <div class="novel-cover">
                    <i class="fas fa-book"></i>
                </div>
                <div class="novel-info">
                    <h3 class="novel-title" title="${novel.name}">${novel.name}</h3>
                    
                    <div class="novel-meta">
                        <div class="novel-stats">
                            <div class="stat-item">
                                <div class="stat-value">${novel.chapter ? novel.chapter.length : 0}</div>
                                <div class="stat-label">章节</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-value">${novel.history || 0}</div>
                                <div class="stat-label">已读</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="novel-progress">
                        <div class="progress-text">
                            ${lastChapterTitle}
                        </div>
                        <div class="progress">
                            <div class="progress-bar" 
                                 role="progressbar" 
                                 style="width: ${progressPercent}%"
                                 aria-valuenow="${progressPercent}" 
                                 aria-valuemin="0" 
                                 aria-valuemax="100">
                            </div>
                        </div>
                    </div>
                    
                    <div class="novel-actions">
                        <button class="btn btn-primary btn-sm continue-reading-btn" 
                                data-novel-id="${novel.id}">
                            <i class="fas fa-play me-1"></i>继续阅读
                        </button>
                        <button class="btn btn-outline-secondary btn-sm start-over-btn" 
                                data-novel-id="${novel.id}">
                            <i class="fas fa-redo me-1"></i>从头开始
                        </button>
                    </div>
                </div>
            </div>
        `;

        // 添加点击事件
        const card = col.querySelector('.novel-card');
        const continueBtn = col.querySelector('.continue-reading-btn');
        const startOverBtn = col.querySelector('.start-over-btn');

        card.addEventListener('click', (e) => {
            // 如果点击的是卡片
            if (e.target.closest('button')) return;
            this.openCatalog(novel.id);
        });
            //点击继续阅读
        continueBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.openNovel(novel.id, novel.history || 0);
        });

        startOverBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (confirm(`确定要从头开始阅读《${novel.name}》吗？`)) {
                this.resetReadingProgress(novel.id);
            }
        });

        return col;
    },

    //打开小说目录
    openCatalog(novelId){
        const novel = this.novels.find(n => n.id === novelId);
        if (novel){
            window.location.href = `catalog.html?novel=${novelId}`
        }
    },


    // 打开小说阅读
    openNovel(novelId, chapterIndex = 0) {
        // 更新阅读历史
        const novel = this.novels.find(n => n.id === novelId);
        if (novel) {
            // 保存到历史记录
            this.addToHistory(novel, chapterIndex);

            // 跳转到阅读页面
            window.location.href = `reading.html?novel=${novelId}&chapter=${chapterIndex}`;
        }
    },

    // 重置阅读进度
    resetReadingProgress(novelId) {
        const novel = this.novels.find(n => n.id === novelId);
        if (novel) {
            novel.history = 0;

            // 更新到后端
            this.updateNovelProgress(novelId, 0);

            // 更新本地显示
            this.renderNovels();
            this.loadHistory();
            this.updateStats();
        }
    },

    // 更新小说阅读进度到后端
    // 更新小说阅读进度到后端
    updateNovelProgress(novelId, chapterIndex) {
        try {
            const novel = this.novels.find(n => n.id === novelId);
            if (!novel || !novel.chapter || !novel.chapter[chapterIndex]) return;

            // 构建请求参数
            const params = new URLSearchParams();
            params.append('chapterNumber', chapterIndex);

            // 如果有章节标题，也发送过去
            const chapterTitle = novel.chapter[chapterIndex];
            if (chapterTitle) {
                params.append('chapterTitle', chapterTitle);
            }

            fetch(`/api/novels/${novelId}/progress`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params.toString()
            })
                .then(response => response.json())
                .then(result => {
                    if (result.success) {
                        console.log(`更新阅读进度成功: ${novel.name} 第${chapterIndex + 1}章`);

                        // 更新本地数据
                        if (novel.history < chapterIndex) {
                            novel.history = chapterIndex;
                        }

                        // 刷新显示
                        this.renderNovels();
                        this.loadHistory();
                        this.updateStats();
                    } else {
                        console.error('更新阅读进度失败:', result.message);
                    }
                })
                .catch(error => {
                    console.error('更新阅读进度请求失败:', error);
                });

        } catch (error) {
            console.error('更新阅读进度失败:', error);
        }
    },

    // 加载历史记录
    // 修改 loadHistory 方法
    loadHistory() {
        try {
            // 从后端API获取历史记录
            fetch('/api/novels/history')
                .then(response => response.json())
                .then(history => {
                    console.log('从后端获取到历史记录:', history);
                    this.history = history;
                    this.renderHistory();

                    // 同时保存到本地存储
                    StorageManager.saveHistory(history);
                })
                .catch(error => {
                    console.error('从后端加载历史记录失败:', error);

                    // 如果后端失败，从本地存储获取
                    this.history = StorageManager.getHistory() || [];
                    this.renderHistory();
                });
        } catch (error) {
            console.error('加载历史记录失败:', error);
            this.history = [];
            this.renderHistory();
        }
    },

    // 渲染历史记录
    // 渲染历史记录
    renderHistory() {
        const historyList = document.getElementById('history-list');
        const clearHistoryBtn = document.getElementById('clearHistoryBtn');

        if (this.history.length === 0) {
            historyList.innerHTML = `
            <li class="history-item text-muted">
                <small>暂无阅读记录</small>
            </li>
        `;
            clearHistoryBtn.classList.add('d-none');
            return;
        }

        // 只显示最近5条记录
        const recentHistory = this.history.slice(0, 5);

        historyList.innerHTML = recentHistory.map(item => {
            // 格式化时间
            const timeText = item.timestamp ?
                this.formatTime(new Date(item.timestamp)) : '刚刚';

            return `
            <li class="history-item" data-novel-id="${item.novelId}" data-chapter="${item.chapterId}">
                <a href="javascript:void(0)" class="history-link">
                    <div class="history-title" title="${item.novelTitle}">${item.novelTitle}</div>
                    <div class="history-chapter" title="${item.chapterTitle}">${item.chapterTitle}</div>
                    <div class="history-time">${timeText}</div>
                </a>
            </li>
        `;
        }).join('');

        // 显示清空按钮
        clearHistoryBtn.classList.remove('d-none');

        // 添加点击事件
        historyList.querySelectorAll('.history-item').forEach(item => {
            item.addEventListener('click', () => {
                const novelId = item.dataset.novelId;
                const chapterIndex = parseInt(item.dataset.chapter) || 0;
                this.openNovel(novelId, chapterIndex);
            });
        });
    },

    // 添加历史记录
    addToHistory(novel, chapterIndex) {
        const historyItem = {
            novelId: novel.id,
            novelTitle: novel.name,
            chapterId: chapterIndex,
            chapterTitle: novel.chapter ? novel.chapter[chapterIndex] : '第' + (chapterIndex + 1) + '章',
            timestamp: new Date().toISOString()
        };

        // 更新本地历史记录
        let history = StorageManager.getHistory() || [];

        // 移除重复记录
        history = history.filter(item =>
            !(item.novelId === novel.id && item.chapterId === chapterIndex)
        );

        // 添加新记录到开头
        history.unshift(historyItem);

        // 只保留最近的50条记录
        history = history.slice(0, 50);

        StorageManager.saveHistory(history);

        // 更新显示
        this.loadHistory();
    },

    // 清空历史记录
    clearHistory() {
        StorageManager.saveHistory([]);
        this.history = [];
        this.renderHistory();
    },

    // 更新统计信息
    updateStats() {
        const totalNovels = this.novels.length;
        const totalChapters = this.novels.reduce((sum, novel) =>
            sum + (novel.chapter ? novel.chapter.length : 0), 0);

        document.getElementById('totalNovels').textContent = totalNovels;
        document.getElementById('totalChapters').textContent = totalChapters;
        document.getElementById('libraryStats').textContent = `共 ${totalNovels} 本小说`;
    },

    // 格式化时间
    formatTime(date) {
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) {
            return '刚刚';
        } else if (diffMins < 60) {
            return `${diffMins}分钟前`;
        } else if (diffHours < 24) {
            return `${diffHours}小时前`;
        } else if (diffDays < 7) {
            return `${diffDays}天前`;
        } else {
            return date.toLocaleDateString('zh-CN');
        }
    }
};

// 初始化应用
document.addEventListener('DOMContentLoaded', () => {
    AppManager.init();
});

// 导出为全局对象
window.AppManager = AppManager;