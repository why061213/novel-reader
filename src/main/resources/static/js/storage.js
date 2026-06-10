const StorageManager = {
    // 获取所有小说
    getNovels() {
        try {
            const novels = localStorage.getItem('novels');
            return novels ? JSON.parse(novels) : [];
        } catch (error) {
            console.error('读取小说数据失败:', error);
            return [];
        }
    },

    // 保存小说
    saveNovels(novels) {
        try {
            localStorage.setItem('novels', JSON.stringify(novels));
        } catch (error) {
            console.error('保存小说数据失败:', error);
        }
    },

    // 获取阅读历史
    getHistory() {
        try {
            const history = localStorage.getItem('readingHistory');
            return history ? JSON.parse(history) : [];
        } catch (error) {
            console.error('读取历史记录失败:', error);
            return [];
        }
    },

    // 保存阅读历史
    saveHistory(history) {
        try {
            localStorage.setItem('readingHistory', JSON.stringify(history));
        } catch (error) {
            console.error('保存历史记录失败:', error);
        }
    },

    // 生成ID
    generateId() {
        return 'novel_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }
};

// 导出为全局对象
window.StorageManager = StorageManager;