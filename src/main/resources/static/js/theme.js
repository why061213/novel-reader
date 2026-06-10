// 主题管理 - 解决闪烁问题
const ThemeManager = {
    // 初始化主题（立即执行，不等待DOMContentLoaded）
    init() {
        // 立即设置主题，防止闪烁
        this.loadThemeImmediately();

        // 延迟设置主题切换监听器
        setTimeout(() => {
            this.setupThemeToggle();
        }, 100);
    },

    // 立即加载主题（在页面渲染前执行）
    loadThemeImmediately() {
        try {
            const savedTheme = localStorage.getItem('theme');
            const htmlElement = document.documentElement;

            // 设置主题到html元素，这样CSS变量会立即生效
            if (savedTheme === 'dark') {
                htmlElement.setAttribute('data-theme', 'dark');
                htmlElement.style.colorScheme = 'dark';
            } else {
                htmlElement.setAttribute('data-theme', 'light');
                htmlElement.style.colorScheme = 'light';
            }

            console.log('立即设置主题:', savedTheme || 'light');
        } catch (error) {
            console.error('立即加载主题失败:', error);
        }
    },

    // 设置主题切换监听器
    setupThemeToggle() {
        const themeToggle = document.getElementById('themeToggle');

        if (!themeToggle) {
            console.warn('主题切换开关未找到');
            return;
        }

        // 设置开关状态
        const savedTheme = localStorage.getItem('theme');
        themeToggle.checked = savedTheme === 'dark';

        // 监听主题切换
        themeToggle.addEventListener('change', (e) => {
            this.toggleTheme(e.target.checked);
        });

        console.log('主题切换监听器已设置');
    },

    // 切换主题
    toggleTheme(isDarkMode) {
        const htmlElement = document.documentElement;

        if (isDarkMode) {
            htmlElement.setAttribute('data-theme', 'dark');
            htmlElement.style.colorScheme = 'dark';
            localStorage.setItem('theme', 'dark');
            console.log('切换到深色模式');
        } else {
            htmlElement.setAttribute('data-theme', 'light');
            htmlElement.style.colorScheme = 'light';
            localStorage.setItem('theme', 'light');
            console.log('切换到浅色模式');
        }

        // 更新主题相关元素
        this.updateThemeDependentElements();
    },

    // 更新主题相关元素
    updateThemeDependentElements() {
        const theme = document.body.getAttribute('data-theme') || 'light';

        // 更新按钮可见性
        this.updateButtonVisibility(theme);

        // 触发自定义事件
        const event = new CustomEvent('themeChanged', { detail: { theme } });
        document.dispatchEvent(event);
    },

    // 更新按钮可见性
    updateButtonVisibility(theme) {
        // 对于白天模式，确保按钮有足够的对比度
        if (theme === 'light') {
            // 为所有主要按钮添加白天模式样式
            const primaryButtons = document.querySelectorAll('.btn-primary');
            primaryButtons.forEach(btn => {
                btn.style.backgroundColor = '#007bff';
                btn.style.borderColor = '#007bff';
                btn.style.color = '#ffffff';
            });

            // 确保上传区域在白天模式下可见
            const uploadAreas = document.querySelectorAll('.upload-area, .drop-zone');
            uploadAreas.forEach(area => {
                area.style.borderColor = '#007bff';
                area.style.backgroundColor = '#f8f9fa';
            });
        }
    }
};

// 立即初始化主题
ThemeManager.init();

// 同时监听DOMContentLoaded，确保主题切换器正常工作
document.addEventListener('DOMContentLoaded', () => {
    // 如果主题切换开关已经设置，就不需要重复设置
    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle && !themeToggle.hasEventListener) {
        // 设置开关状态
        const savedTheme = localStorage.getItem('theme');
        themeToggle.checked = savedTheme === 'dark';

        // 添加事件监听器
        themeToggle.addEventListener('change', (e) => {
            ThemeManager.toggleTheme(e.target.checked);
        });

        themeToggle.hasEventListener = true;
    }
});

// 导出为全局对象
window.ThemeManager = ThemeManager;