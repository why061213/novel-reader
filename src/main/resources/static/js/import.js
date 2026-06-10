// 导入页面逻辑 - 简洁版
const ImportManager = {
    // 初始化
    init() {
        this.selectedFile = null;
        this.isUploading = false;
        this.setupEventListeners();
        // 移除 loadLibraryStats() 调用
    },

    // 设置事件监听器
    setupEventListeners() {
        const uploadArea = document.getElementById('uploadArea');
        const fileInput = document.getElementById('fileInput');
        const uploadBtn = document.getElementById('uploadBtn');

        // 点击上传区域选择文件
        uploadArea.addEventListener('click', () => {
            fileInput.click();
        });

        // 文件选择变化
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileSelect(e.target.files[0]);
            }
        });

        // 拖放功能
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.style.borderColor = 'var(--primary-color)';
            uploadArea.style.backgroundColor = 'var(--sidebar-hover)';
        });

        uploadArea.addEventListener('dragleave', (e) => {
            e.preventDefault();
            uploadArea.style.borderColor = 'var(--card-border)';
            uploadArea.style.backgroundColor = 'var(--bg-color-secondary)';
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.style.borderColor = 'var(--card-border)';
            uploadArea.style.backgroundColor = 'var(--bg-color-secondary)';

            if (e.dataTransfer.files.length > 0) {
                this.handleFileSelect(e.dataTransfer.files[0]);
            }
        });

        // 上传按钮点击
        uploadBtn.addEventListener('click', () => {
            this.startUpload();
        });

        // 查看小说按钮
        document.getElementById('viewNovelBtn').addEventListener('click', () => {
            window.location.href = 'index.html';
        });

        // 继续导入按钮
        document.getElementById('importAnotherBtn').addEventListener('click', () => {
            this.resetForm();
        });
    },

    // 处理文件选择
    handleFileSelect(file) {
        // 验证文件类型
        if (!file.name.toLowerCase().endsWith('.txt')) {
            this.showError('请选择TXT格式的文件');
            return;
        }

        // 验证文件大小（限制100MB）
        const maxSize = 100 * 1024 * 1024; // 100MB
        if (file.size > maxSize) {
            this.showError('文件过大，请选择小于100MB的文件');
            return;
        }

        this.selectedFile = file;
        this.displayFileInfo(file);
        document.getElementById('uploadBtn').disabled = false;

        // 隐藏错误信息（如果有）
        document.getElementById('errorAlert').style.display = 'none';
    },

    // 显示文件信息
    displayFileInfo(file) {
        document.getElementById('selectedFileInfo').style.display = 'block';
        document.getElementById('fileName').textContent = file.name;
        document.getElementById('fileSize').textContent = this.formatFileSize(file.size);
    },

    // 格式化文件大小
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    // 开始上传
    async startUpload() {
        if (!this.selectedFile || this.isUploading) return;

        this.isUploading = true;

        // 更新UI状态
        document.getElementById('uploadBtn').disabled = true;
        document.getElementById('uploadBtn').innerHTML =
            '<span class="spinner-border spinner-border-sm me-2"></span>上传中...';

        // 显示进度指示器
        document.getElementById('progressIndicator').style.display = 'block';

        // 隐藏错误信息（如果有）
        document.getElementById('errorAlert').style.display = 'none';

        try {
            // 创建FormData对象
            const formData = new FormData();
            formData.append('file', this.selectedFile);

            // 模拟进度更新
            this.updateProgress(0, '准备上传...');
            await this.delay(500);

            this.updateProgress(30, '正在上传文件...');

            // 发送到后端API
            const response = await fetch('/api/upload/simple', {
                method: 'POST',
                body: formData
            });

            const result = await response.json();

            if (!result.success) {
                throw new Error(result.message || '上传失败');
            }

            this.updateProgress(70, '文件上传完成，正在保存...');
            await this.delay(1000);

            this.updateProgress(100, '上传成功！');

            // 显示成功消息和操作按钮
            this.showSuccess(result.message || '小说导入成功');

        } catch (error) {
            console.error('上传失败:', error);
            this.handleUploadError(error.message || '上传过程中发生错误');
        }
    },

    // 显示成功消息
    showSuccess(message) {
        // 隐藏上传按钮和进度指示器
        document.getElementById('uploadBtn').style.display = 'none';
        document.getElementById('progressIndicator').style.display = 'none';

        // 显示成功消息
        const successHtml = `
            <div class="alert alert-success" role="alert">
                <i class="fas fa-check-circle me-2"></i>
                <strong>上传成功！</strong> ${message}
            </div>
        `;

        const successDiv = document.createElement('div');
        successDiv.innerHTML = successHtml;
        document.getElementById('selectedFileInfo').parentNode.insertBefore(successDiv, document.getElementById('selectedFileInfo').nextSibling);

        // 显示操作按钮
        document.getElementById('actionButtons').style.display = 'flex';
    },

    // 完成导入
    finishImport(novelId, chapterCount) {
        return new Promise((resolve) => {
            // 显示完成界面
            document.getElementById('importForm').style.display = 'none';
            document.getElementById('importFinished').style.display = 'block';

            // 更新成功消息
            const message = `小说已成功导入！<br>
                        共 ${chapterCount} 章。<br>
                        3秒后自动跳转到主页...`;
            document.getElementById('successMessage').innerHTML = message;

            this.importing = false;

            // 3秒后自动跳转
            this.autoRedirectTimer = setTimeout(() => {
                window.location.href = 'index.html';
            }, 3000);

            resolve();
        });
    },

    // 更新进度
    updateProgress(percent, text) {
        const progressBar = document.getElementById('progressBar');
        const progressText = document.getElementById('progressText');

        progressBar.style.width = percent + '%';
        progressBar.setAttribute('aria-valuenow', percent);
        progressText.textContent = text;
    },

    // 处理上传错误
    handleUploadError(errorMessage) {
        this.isUploading = false;

        // 恢复上传按钮
        document.getElementById('uploadBtn').disabled = false;
        document.getElementById('uploadBtn').innerHTML =
            '<i class="fas fa-upload me-2"></i>重新上传';

        // 显示错误信息
        this.showError(errorMessage);

        // 隐藏进度指示器
        document.getElementById('progressIndicator').style.display = 'none';
    },

    // 显示错误信息
    showError(message) {
        const errorAlert = document.getElementById('errorAlert');
        const errorMessage = document.getElementById('errorMessage');

        errorMessage.textContent = message;
        errorAlert.style.display = 'block';
    },

    // 重置表单
    resetForm() {
        this.selectedFile = null;
        this.isUploading = false;

        // 重置UI
        document.getElementById('selectedFileInfo').style.display = 'none';
        document.getElementById('progressIndicator').style.display = 'none';
        document.getElementById('uploadBtn').disabled = true;
        document.getElementById('uploadBtn').innerHTML =
            '<i class="fas fa-upload me-2"></i>开始上传';
        document.getElementById('uploadBtn').style.display = 'block';
        document.getElementById('errorAlert').style.display = 'none';
        document.getElementById('actionButtons').style.display = 'none';

        // 移除可能存在的成功消息
        const successAlert = document.querySelector('.alert-success');
        if (successAlert) {
            successAlert.remove();
        }

        // 重置文件输入
        document.getElementById('fileInput').value = '';

        // 重置进度条
        this.updateProgress(0, '');
    },

    // 延迟函数
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
};

// 初始化导入页面
document.addEventListener('DOMContentLoaded', () => {
    ImportManager.init();
});

// 导出为全局对象
window.ImportManager = ImportManager;