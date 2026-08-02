# 定时提醒系统

基于数据库的智能定时提醒管理平台，支持针对数据库表数据生成定时提醒任务，通过钉钉机器人发送通知。

## 技术栈

- **后端**: JDK 8 + Spring Boot 2.7.18 + MyBatis Plus 3.5.3.2 + Spring Task
- **前端**: Vue 3 + Element Plus（CDN引入，嵌入HTML，前后端不分离部署）
- **数据库**: MySQL 8.0（utf8mb4 编码）
- **部署**: Docker + Docker Compose

## 功能特性

### 1. 任务管理
- 针对数据库任意表创建定时提醒任务
- 支持任务的新建、编辑、删除、启用/停用
- 支持手动触发执行任务
- 管理员可查看所有任务，普通用户只能查看自己创建的任务

### 2. 执行频率配置
- 支持**每日**、**每周**、**每月**三种频率
- 每周/每月可选择具体日期
- 精确到小时和分钟

### 3. 数据触发条件
- **可视化模式**：通过下拉框选择字段、操作符、比较值，支持多个条件的 AND/OR 组合
  - 操作符支持：大于、大于等于、小于、小于等于、等于、不等于、包含、不包含、开头是
  - 比较值支持：固定值或其他字段
- **编辑器模式**：直接编写 WHERE 语句，提供字段列表点击插入功能
- 支持查询结果预览

### 4. 钉钉提醒
- 支持配置钉钉机器人 Webhook 地址
- 消息模板支持固定文本与 `${字段名}` 占位符混合
- 指定数据表字段作为消息接收方

### 5. 发送日志
- 记录每次任务执行的完整信息
- 包含：任务ID、任务名称、发送时间、接收方、发送内容、状态
- 支持搜索和分页查看

### 6. 用户权限
- 管理员（ADMIN）：可查看和管理所有任务
- 普通用户（USER）：仅可管理自己创建的任务

## 快速启动

### Docker 一键启动

```bash
# 进入项目目录
cd label-1144

# 启动服务（首次启动需要构建镜像）
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看应用日志
docker-compose logs -f app

# 停止服务
docker-compose down

# 停止服务并清除数据（重新初始化数据库）
docker-compose down -v
```

> **注意**：首次启动时 MySQL 需要约 30-60 秒初始化，应用会自动等待 MySQL 就绪后再启动。

### 访问地址

- **系统地址**: http://localhost:1144
- **MySQL 端口**: 11441（外部访问）
- **默认账号**:

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 可管理所有任务 |
| 普通用户 | zhangsan | user123 | 仅管理自己的任务 |
| 普通用户 | lisi | user123 | 仅管理自己的任务 |

## 测试流程

### 一、用户认证测试

1. **登录测试**
   - 打开浏览器访问 `http://localhost:1144`
   - 使用管理员账号 `admin / admin123` 登录
   - 验证：登录成功后跳转到任务管理页面，左侧导航栏显示用户信息
   
2. **权限测试**
   - 使用 `admin` 登录，创建一个任务
   - 退出登录，用 `zhangsan` 登录
   - 验证：`zhangsan` 只能看到自己创建的任务，看不到 `admin` 创建的任务

3. **登出测试**
   - 点击左下角退出按钮
   - 验证：成功跳转回登录页面

### 二、任务管理测试

4. **创建任务**
   - 进入「任务管理」页面，点击「新建任务」按钮
   - 填写任务名称，如"合同到期提醒"
   - 选择目标数据表，如 `demo_employee`
   - 验证：选择表后，条件区域的字段下拉框自动加载对应表的列信息

5. **配置执行频率**
   - 选择频率类型为「每日」，设定执行时间（时/分）
   - 切换到「每周」模式，选择星期几
   - 切换到「每月」模式，选择日期
   - 验证：各频率类型切换正常，时间设置可选

6. **配置数据触发条件（可视化模式）**
   - 条件模式选择「可视化模式」
   - 在第一个条件中：选择字段 → 选择操作符（如"等于"） → 选择比较类型（固定值/字段） → 输入比较值
   - 点击「+ 添加条件」，添加第二个条件
   - 设置两个条件之间的逻辑关系（并且/或者）
   - 验证：所有下拉框选项展示正确，条件添加和删除正常

7. **配置数据触发条件（编辑器模式）**
   - 切换到「编辑器模式」
   - 在 SQL 编辑框中输入 WHERE 条件，如 `status = '在职' AND contract_end < '2025-06-01'`
   - 点击右侧字段列表中的字段名称，验证自动插入到编辑器
   - 验证：SQL 输入正常，字段点击插入正常

8. **预览查询结果**
   - 点击「预览查询结果」按钮
   - 验证：弹出预览面板，展示匹配条件的数据记录和生成的 SQL 语句

9. **配置消息模板**
   - 在消息模板区域输入模板文本
   - 点击字段标签插入占位符，如 `${name} 的合同将于 ${contract_end} 到期，请及时续签`
   - 选择接收方字段，如 `phone`
   - 验证：字段占位符插入正确

10. **配置钉钉通知**
    - 填入钉钉机器人 Webhook 地址
    - 验证：地址输入框正常

11. **保存任务**
    - 点击「确定」保存任务
    - 验证：任务保存成功，任务列表中出现新创建的任务

12. **编辑任务**
    - 在任务列表中点击某任务的「编辑」按钮
    - 修改任务名称或条件
    - 点击保存
    - 验证：修改后的内容已更新

13. **启用/停用任务**
    - 在任务列表中点击「停用」按钮
    - 验证：任务状态变为「已停用」
    - 再次点击「启用」
    - 验证：任务状态变为「运行中」

14. **手动执行任务**
    - 点击任务列表中的「执行」按钮
    - 验证：提示执行成功

15. **删除任务**
    - 点击「删除」按钮
    - 弹出确认对话框，点击确认
    - 验证：任务从列表中移除

### 三、发送日志测试

16. **查看日志**
    - 切换到「发送日志」页面
    - 验证：日志表格正常展示为横向列布局（ID、任务ID、任务名称、发送时间、接收方、发送内容、状态、错误信息）
    
17. **搜索日志**
    - 在搜索框输入关键字，点击搜索
    - 验证：搜索结果按关键字过滤
    - 点击重置
    - 验证：搜索清空，显示全部日志

18. **分页功能**
    - 如有多条日志，验证分页切换正常
    - 切换每页条数（15/30/50），验证生效

### 四、数据库元数据测试

19. **表列表加载**
    - 创建任务时，下拉框应自动加载所有数据库表
    - 验证：至少包含 `demo_employee`、`demo_order`、`demo_project` 三张示例表

20. **字段信息加载**
    - 选择一张表后，条件区域和消息模板区域应展示该表的字段列表
    - 验证：字段名和注释信息正确显示

### 五、安全性测试

21. **SQL 注入防护**
    - 在编辑器模式中尝试输入危险 SQL，如 `1=1; DROP TABLE demo_employee`
    - 验证：系统拒绝执行并提示安全错误

22. **会话验证**
    - 不登录直接访问 API，如 `http://localhost:1144/api/task/page`
    - 验证：返回未登录错误提示

## 项目结构

```
label-1144/
├── docker-compose.yml              # Docker 编排配置
├── Dockerfile                      # Docker 镜像构建（多阶段构建）
├── pom.xml                         # Maven 项目配置
├── README.md                       # 项目说明
├── sql/
│   └── init.sql                    # 数据库初始化脚本（含示例数据）
└── src/main/
    ├── java/com/example/reminder/
    │   ├── ReminderApplication.java    # 启动类
    │   ├── config/                     # 配置类
    │   │   ├── AuthInterceptor.java    # Session 认证拦截器
    │   │   ├── MyBatisPlusConfig.java  # MyBatis Plus 分页+自动填充
    │   │   ├── SchedulerConfig.java    # ThreadPoolTaskScheduler 配置
    │   │   └── WebConfig.java          # Web MVC 配置
    │   ├── controller/                 # 控制器
    │   │   ├── LogController.java      # 日志查询接口
    │   │   ├── MetadataController.java # 数据库元数据接口
    │   │   ├── TaskController.java     # 任务管理接口
    │   │   └── UserController.java     # 用户认证接口
    │   ├── entity/                     # 实体类
    │   │   ├── ReminderCondition.java  # 触发条件
    │   │   ├── ReminderLog.java        # 发送日志
    │   │   ├── ReminderTask.java       # 提醒任务
    │   │   └── SysUser.java            # 系统用户
    │   ├── mapper/                     # MyBatis Plus 数据访问
    │   │   ├── ReminderConditionMapper.java
    │   │   ├── ReminderLogMapper.java
    │   │   ├── ReminderTaskMapper.java
    │   │   └── SysUserMapper.java
    │   ├── service/                    # 业务逻辑层
    │   │   ├── ConditionService.java   # 条件构建与 SQL 拼接
    │   │   ├── DingTalkService.java    # 钉钉 Webhook 消息发送
    │   │   ├── MetadataService.java    # 数据库表/列元数据查询
    │   │   ├── ReminderLogService.java # 日志记录与查询
    │   │   ├── SchedulerService.java   # 动态任务调度核心
    │   │   ├── TaskService.java        # 任务 CRUD
    │   │   └── UserService.java        # 用户认证
    │   └── util/                       # 工具类
    │       ├── CronUtils.java          # Cron 表达式生成
    │       ├── Result.java             # 统一响应封装
    │       └── SqlSanitizer.java       # SQL 注入防护
    └── resources/
        ├── application.yml             # 应用配置
        └── static/                     # 前端静态资源
            ├── index.html              # 单页面应用
            ├── css/style.css           # 全局样式
            └── js/app.js               # Vue 3 应用逻辑
```

## API 接口

### 用户认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/login | 用户登录（参数：username, password） |
| POST | /api/user/logout | 用户登出 |
| GET | /api/user/current | 获取当前登录用户信息 |

### 任务管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/task/page | 分页查询任务（支持搜索） |
| GET | /api/task/{id} | 获取任务详情（含条件配置） |
| POST | /api/task | 创建新任务 |
| PUT | /api/task/{id} | 更新任务 |
| DELETE | /api/task/{id} | 删除任务 |
| POST | /api/task/{id}/toggle | 切换任务启用/停用状态 |
| POST | /api/task/{id}/trigger | 手动触发执行任务 |
| POST | /api/task/preview-sql | 预览条件查询 SQL 及结果 |

### 数据库元数据

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/metadata/tables | 获取所有业务表列表 |
| GET | /api/metadata/columns/{table} | 获取指定表的字段信息 |
| GET | /api/metadata/preview/{table} | 预览表数据（前10条） |

### 发送日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/log/page | 分页查询发送日志（支持搜索） |

## 示例业务表

系统内置三张示例表供测试：

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| demo_employee | 员工表 | 姓名、手机、部门、职位、薪资、入职日期、合同到期日、状态 |
| demo_order | 订单表 | 订单号、客户名称、客户手机、金额、状态、截止日期 |
| demo_project | 项目表 | 项目名称、负责人、负责人手机、预算、已花费、截止日期、进度 |

## 常见问题

### MySQL 启动慢导致应用连接失败
MySQL 首次初始化需要时间。已通过 Docker healthcheck 配置自动等待。如仍失败，执行：
```bash
docker-compose down -v
docker-compose up -d --build
```

### 中文乱码
已在 MySQL 启动参数和初始化脚本中配置 `utf8mb4` 编码。如遇乱码，需清除旧数据卷重新初始化：
```bash
docker-compose down -v
docker-compose up -d --build
```

### 端口冲突
- 应用端口 `1144`，如冲突可在 `docker-compose.yml` 中修改 `ports` 映射
- MySQL 外部端口 `11441`，如冲突同理修改
