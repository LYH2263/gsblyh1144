/* =============================================
   定时提醒系统 - Vue3 应用主文件
   ============================================= */

const { createApp, ref, reactive, computed, onMounted, watch, nextTick, toRaw } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

const app = createApp({
    setup() {
        // ============= 用户认证 =============
        const currentUser = ref(null);
        const loginForm = reactive({ username: '', password: '' });
        const loginLoading = ref(false);

        const checkLogin = async () => {
            try {
                const res = await fetch('/api/user/current');
                const data = await res.json();
                if (data.code === 200) {
                    currentUser.value = data.data;
                }
            } catch (e) { /* 未登录 */ }
        };

        const handleLogin = async () => {
            if (!loginForm.username || !loginForm.password) {
                ElMessage.warning('请输入用户名和密码');
                return;
            }
            loginLoading.value = true;
            try {
                const res = await fetch('/api/user/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(loginForm)
                });
                const data = await res.json();
                if (data.code === 200) {
                    currentUser.value = data.data;
                    ElMessage.success('登录成功');
                    loadTables();
                    loadTasks();
                } else {
                    ElMessage.error(data.message || '登录失败');
                }
            } catch (e) {
                ElMessage.error('网络错误');
            }
            loginLoading.value = false;
        };

        const handleLogout = async () => {
            await fetch('/api/user/logout', { method: 'POST' });
            currentUser.value = null;
            ElMessage.success('已退出登录');
        };

        // ============= 导航 =============
        const activeMenu = ref('tasks');

        // ============= 下拉选项数据 =============
        const operatorOptions = [
            { label: '等于', value: 'EQ' },
            { label: '不等于', value: 'NEQ' },
            { label: '大于', value: 'GT' },
            { label: '大于等于', value: 'GTE' },
            { label: '小于', value: 'LT' },
            { label: '小于等于', value: 'LTE' },
            { label: '包含', value: 'CONTAINS' },
            { label: '不包含', value: 'NOT_CONTAINS' },
            { label: '开头是', value: 'STARTS_WITH' }
        ];
        const compareTypeOptions = [
            { label: '固定值', value: 'VALUE' },
            { label: '字段', value: 'FIELD' }
        ];

        // ============= 数据库元数据 =============
        const tables = ref([]);
        const currentColumns = ref([]);

        const loadTables = async () => {
            try {
                const res = await fetch('/api/metadata/tables');
                const data = await res.json();
                if (data.code === 200) {
                    tables.value = data.data;
                }
            } catch (e) { console.error('加载表列表失败', e); }
        };

        const loadColumns = async (tableName) => {
            if (!tableName) { currentColumns.value = []; return; }
            try {
                const res = await fetch('/api/metadata/columns/' + tableName);
                const data = await res.json();
                if (data.code === 200) {
                    currentColumns.value = data.data;
                }
            } catch (e) { console.error('加载字段列表失败', e); }
        };

        // ============= 任务列表 =============
        const taskList = ref([]);
        const taskLoading = ref(false);
        const taskSearch = ref('');
        const taskPage = ref(1);
        const taskPageSize = ref(10);
        const taskTotal = ref(0);

        const loadTasks = async () => {
            taskLoading.value = true;
            try {
                const params = new URLSearchParams({
                    pageNum: taskPage.value,
                    pageSize: taskPageSize.value
                });
                if (taskSearch.value) params.append('keyword', taskSearch.value);
                const res = await fetch('/api/task/page?' + params);
                const data = await res.json();
                if (data.code === 200) {
                    taskList.value = data.data.records;
                    taskTotal.value = data.data.total;
                }
            } catch (e) { console.error('加载任务列表失败', e); }
            taskLoading.value = false;
        };

        const getFrequencyText = (row) => {
            const h = (row.frequencyHour < 10 ? '0' : '') + row.frequencyHour;
            const m = (row.frequencyMinute < 10 ? '0' : '') + row.frequencyMinute;
            const time = h + ':' + m;
            if (row.frequencyType === 'DAILY') return '每天 ' + time;
            if (row.frequencyType === 'WEEKLY') {
                const days = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];
                return '每' + (days[row.frequencyDay] || '周一') + ' ' + time;
            }
            if (row.frequencyType === 'MONTHLY') {
                return '每月' + row.frequencyDay + '日 ' + time;
            }
            return time;
        };

        const toggleTask = async (id) => {
            try {
                const res = await fetch('/api/task/' + id + '/toggle', { method: 'POST' });
                const data = await res.json();
                if (data.code === 200) {
                    ElMessage.success('状态切换成功');
                    loadTasks();
                } else {
                    ElMessage.error(data.message);
                }
            } catch (e) { ElMessage.error('操作失败'); }
        };

        const triggerTask = async (id) => {
            try {
                await ElMessageBox.confirm('确定要立即执行此任务吗？', '确认执行', { type: 'warning' });
                const res = await fetch('/api/task/' + id + '/trigger', { method: 'POST' });
                const data = await res.json();
                if (data.code === 200) {
                    ElMessage.success('任务已触发执行，请查看发送日志');
                } else {
                    ElMessage.error(data.message);
                }
            } catch (e) {
                if (e !== 'cancel') ElMessage.error('操作失败');
            }
        };

        const deleteTask = async (row) => {
            try {
                await ElMessageBox.confirm('确定要删除任务"' + row.taskName + '"吗？删除后不可恢复', '确认删除', { type: 'danger' });
                const res = await fetch('/api/task/' + row.id, { method: 'DELETE' });
                const data = await res.json();
                if (data.code === 200) {
                    ElMessage.success('删除成功');
                    loadTasks();
                } else {
                    ElMessage.error(data.message);
                }
            } catch (e) {
                if (e !== 'cancel') ElMessage.error('删除失败');
            }
        };

        // ============= 任务表单 =============
        const taskFormVisible = ref(false);
        const taskFormMode = ref('create');
        const saveLoading = ref(false);
        const taskForm = reactive({
            id: null,
            taskName: '',
            targetTable: '',
            frequencyType: 'DAILY',
            frequencyDay: null,
            frequencyHour: 9,
            frequencyMinute: 0,
            conditionMode: 'VISUAL',
            conditionSql: '',
            messageTemplate: '',
            receiverField: '',
            dingtalkWebhook: '',
            statusBool: true,
            conditions: [
                { fieldName: '', operator: 'EQ', compareType: 'VALUE', compareValue: '', logicOperator: 'AND' }
            ]
        });

        const resetTaskForm = () => {
            taskForm.id = null;
            taskForm.taskName = '';
            taskForm.targetTable = '';
            taskForm.frequencyType = 'DAILY';
            taskForm.frequencyDay = null;
            taskForm.frequencyHour = 9;
            taskForm.frequencyMinute = 0;
            taskForm.conditionMode = 'VISUAL';
            taskForm.conditionSql = '';
            taskForm.messageTemplate = '';
            taskForm.receiverField = '';
            taskForm.dingtalkWebhook = '';
            taskForm.statusBool = true;
            taskForm.conditions = [
                { fieldName: '', operator: 'EQ', compareType: 'VALUE', compareValue: '', logicOperator: 'AND' }
            ];
            previewData.value = null;
            currentColumns.value = [];
        };

        const openTaskForm = async (taskId) => {
            resetTaskForm();
            if (tables.value.length === 0) await loadTables();

            if (taskId) {
                taskFormMode.value = 'edit';
                try {
                    const res = await fetch('/api/task/' + taskId);
                    const data = await res.json();
                    if (data.code === 200) {
                        const task = data.data.task;
                        const conditions = data.data.conditions;

                        taskForm.id = task.id;
                        taskForm.taskName = task.taskName;
                        taskForm.targetTable = task.targetTable;
                        taskForm.frequencyType = task.frequencyType;
                        taskForm.frequencyDay = task.frequencyDay;
                        taskForm.frequencyHour = task.frequencyHour;
                        taskForm.frequencyMinute = task.frequencyMinute;
                        taskForm.conditionMode = task.conditionMode;
                        taskForm.conditionSql = task.conditionSql || '';
                        taskForm.messageTemplate = task.messageTemplate;
                        taskForm.receiverField = task.receiverField;
                        taskForm.dingtalkWebhook = task.dingtalkWebhook || '';
                        taskForm.statusBool = task.status === 1;

                        if (conditions && conditions.length > 0) {
                            taskForm.conditions = conditions.map(c => ({
                                fieldName: c.fieldName,
                                operator: c.operator,
                                compareType: c.compareType || 'VALUE',
                                compareValue: c.compareValue || '',
                                logicOperator: c.logicOperator || 'AND'
                            }));
                        }

                        if (task.targetTable) {
                            await loadColumns(task.targetTable);
                        }
                    }
                } catch (e) { ElMessage.error('加载任务详情失败'); }
            } else {
                taskFormMode.value = 'create';
            }
            taskFormVisible.value = true;
        };

        const onTableChange = async (val) => {
            await loadColumns(val);
            // 清空依赖表的字段选择
            taskForm.receiverField = '';
            taskForm.conditions.forEach(c => {
                c.fieldName = '';
                if (c.compareType === 'FIELD') c.compareValue = '';
            });
        };

        const addCondition = () => {
            taskForm.conditions.push({
                fieldName: '', operator: 'EQ', compareType: 'VALUE', compareValue: '', logicOperator: 'AND'
            });
        };

        const removeCondition = (idx) => {
            if (taskForm.conditions.length > 1) {
                taskForm.conditions.splice(idx, 1);
            }
        };

        // SQL编辑器 - 插入字段名
        const sqlTextarea = ref(null);
        const insertField = (fieldName) => {
            const el = document.getElementById('sql-textarea');
            if (!el) return;
            const start = el.selectionStart;
            const end = el.selectionEnd;
            const before = taskForm.conditionSql.substring(0, start);
            const after = taskForm.conditionSql.substring(end);
            taskForm.conditionSql = before + '`' + fieldName + '`' + after;
            nextTick(() => {
                const pos = start + fieldName.length + 2;
                el.setSelectionRange(pos, pos);
                el.focus();
            });
        };

        // 消息模板 - 插入字段占位符
        const insertMessageField = (fieldName) => {
            const el = document.getElementById('input-message-template');
            if (!el) {
                taskForm.messageTemplate += '${' + fieldName + '}';
                return;
            }
            const textarea = el.querySelector('textarea');
            if (!textarea) {
                taskForm.messageTemplate += '${' + fieldName + '}';
                return;
            }
            const start = textarea.selectionStart;
            const end = textarea.selectionEnd;
            const before = taskForm.messageTemplate.substring(0, start);
            const after = taskForm.messageTemplate.substring(end);
            const insert = '${' + fieldName + '}';
            taskForm.messageTemplate = before + insert + after;
            nextTick(() => {
                const pos = start + insert.length;
                textarea.setSelectionRange(pos, pos);
                textarea.focus();
            });
        };

        // 预览查询
        const previewData = ref(null);
        const previewColumns = computed(() => {
            if (!previewData.value || !previewData.value.data || previewData.value.data.length === 0) return [];
            return Object.keys(previewData.value.data[0]);
        });
        const previewLoading = ref(false);

        const previewQuery = async () => {
            if (!taskForm.targetTable) {
                ElMessage.warning('请先选择目标数据表');
                return;
            }
            previewLoading.value = true;
            try {
                const body = {
                    targetTable: taskForm.targetTable,
                    conditionMode: taskForm.conditionMode
                };
                if (taskForm.conditionMode === 'EDITOR') {
                    body.conditionSql = taskForm.conditionSql;
                } else {
                    body.conditions = taskForm.conditions.filter(c => c.fieldName);
                }
                const res = await fetch('/api/task/preview-sql', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                const data = await res.json();
                if (data.code === 200) {
                    previewData.value = data.data;
                } else {
                    ElMessage.error(data.message);
                }
            } catch (e) { ElMessage.error('预览失败'); }
            previewLoading.value = false;
        };

        // 保存任务
        const saveTask = async () => {
            if (!taskForm.taskName) { ElMessage.warning('请输入任务名称'); return; }
            if (!taskForm.targetTable) { ElMessage.warning('请选择目标数据表'); return; }
            if (taskForm.frequencyType !== 'DAILY' && !taskForm.frequencyDay) { ElMessage.warning('请选择执行日期'); return; }
            if (taskForm.frequencyHour === null || taskForm.frequencyHour === undefined) { ElMessage.warning('请选择执行小时'); return; }
            if (taskForm.frequencyMinute === null || taskForm.frequencyMinute === undefined) { ElMessage.warning('请选择执行分钟'); return; }
            if (!taskForm.receiverField) { ElMessage.warning('请选择接收方字段'); return; }
            if (!taskForm.messageTemplate) { ElMessage.warning('请输入消息模板'); return; }

            saveLoading.value = true;
            try {
                const body = {
                    taskName: taskForm.taskName,
                    targetTable: taskForm.targetTable,
                    frequencyType: taskForm.frequencyType,
                    frequencyDay: taskForm.frequencyDay,
                    frequencyHour: taskForm.frequencyHour,
                    frequencyMinute: taskForm.frequencyMinute,
                    conditionMode: taskForm.conditionMode,
                    conditionSql: taskForm.conditionMode === 'EDITOR' ? taskForm.conditionSql : null,
                    messageTemplate: taskForm.messageTemplate,
                    receiverField: taskForm.receiverField,
                    dingtalkWebhook: taskForm.dingtalkWebhook || null,
                    status: taskForm.statusBool ? 1 : 0
                };

                if (taskForm.conditionMode === 'VISUAL') {
                    body.conditions = taskForm.conditions.filter(c => c.fieldName);
                }

                let url = '/api/task';
                let method = 'POST';
                if (taskFormMode.value === 'edit') {
                    url = '/api/task/' + taskForm.id;
                    method = 'PUT';
                }

                const res = await fetch(url, {
                    method,
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                const data = await res.json();
                if (data.code === 200) {
                    ElMessage.success(data.message || '保存成功');
                    taskFormVisible.value = false;
                    loadTasks();
                } else {
                    ElMessage.error(data.message || '保存失败');
                }
            } catch (e) { ElMessage.error('保存失败'); }
            saveLoading.value = false;
        };

        // ============= 发送日志 =============
        const logList = ref([]);
        const logLoading = ref(false);
        const logSearch = ref('');
        const logPage = ref(1);
        const logPageSize = ref(15);
        const logTotal = ref(0);

        const loadLogs = async () => {
            logLoading.value = true;
            try {
                const params = new URLSearchParams({
                    pageNum: logPage.value,
                    pageSize: logPageSize.value
                });
                if (logSearch.value) params.append('keyword', logSearch.value);
                const res = await fetch('/api/log/page?' + params);
                const data = await res.json();
                if (data.code === 200) {
                    logList.value = data.data.records;
                    logTotal.value = data.data.total;
                }
            } catch (e) { console.error('加载日志失败', e); }
            logLoading.value = false;
        };

        // ============= 初始化 =============
        onMounted(async () => {
            await checkLogin();
            if (currentUser.value) {
                loadTables();
                loadTasks();
            }
        });

        return {
            // 用户认证
            currentUser, loginForm, loginLoading,
            handleLogin, handleLogout,
            // 导航
            activeMenu,
            // 下拉选项
            operatorOptions, compareTypeOptions,
            // 元数据
            tables, currentColumns,
            // 任务列表
            taskList, taskLoading, taskSearch, taskPage, taskPageSize, taskTotal,
            loadTasks, getFrequencyText, toggleTask, triggerTask, deleteTask,
            // 任务表单
            taskFormVisible, taskFormMode, taskForm, saveLoading,
            openTaskForm, onTableChange, addCondition, removeCondition,
            insertField, insertMessageField, sqlTextarea,
            previewData, previewColumns, previewLoading, previewQuery,
            saveTask,
            // 日志
            logList, logLoading, logSearch, logPage, logPageSize, logTotal,
            loadLogs
        };
    }
});

app.use(ElementPlus);
app.mount('#app');
