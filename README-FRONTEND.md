# SavoryLife 前端项目统一说明

本项目存在两套前端代码。请使用 **savory-life/** 目录下的版本，外部根目录的同名目录是之前会话自动生成的旧版本，与当前后端代码不完全对齐。

## 当前使用的版本（推荐）

| 项目 | 路径 | 端口 | 状态 |
|------|------|:--:|:--:|
| 管理端 | `savory-life/savory-admin/` | 5173 | 6页完整 + 6页框架占位 |
| 商家端 | `savory-life/savory-merchant/` | 5174 | 7页全部实现 |
| 数据大屏 | `savory-life/savory-screen/` | 5175 | App.vue 单文件大屏 |
| 小程序 | `savory-life/savory-miniapp/` | - | 5页已实现 + 缺少核心配置文件 |

## 旧版本（与后端路由不一致，保留作为参考）

| 项目 | 路径 | 说明 |
|------|------|------|
| 管理端 | `savory-admin/` | 顶层，11页完整，但路由/Spec与后端未对齐 |
| 商家端 | `savory-merchant/` | 顶层，5页，无 Dashboard 和 ShopInfo |
| 数据大屏 | `savory-screen/` | 顶层，Vite 脚手架代码，无大屏业务代码 |

## 小程序问题

`savory-miniapp/` 有 5 个页面代码但缺少 uni-app 核心配置文件（pages.json, manifest.json, App.vue, main.js）。
login/shop/note 三个页面目录为空。
需要使用 HBuilder X 创建标准 uni-app 项目后，把这些页面代码迁移过去。

## 启动方式

```bash
# 管理端
cd savory-life/savory-admin && npm install && npm run dev

# 商家端
cd savory-life/savory-merchant && npm install && npm run dev

# 数据大屏
cd savory-life/savory-screen && npm install && npm run dev
```
