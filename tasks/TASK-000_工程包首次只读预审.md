# TASK-000 工程包首次只读预审

## 任务目标

本任务是 Codex 第一次接触本工程包时必须执行的只读审查任务。目标是发现工程包内部的字段、接口、权限、状态机、任务顺序、验收用例之间的矛盾，降低后续开发跑偏风险。

## 执行方式

复制并执行：

```text
prompts/Codex首次只读预审提示词.md
```

## 禁止事项

- 不允许写代码；
- 不允许修改任何文件；
- 不允许初始化后端、前端或小程序项目；
- 不允许执行 `TASK-001`；
- 不允许自行更换技术栈；
- 不允许跳过 `AGENTS.md` 和 `docs/00-开发前冻结事项.md`。

## 输入文件

- `AGENTS.md`
- `README.md`
- `MANIFEST.md`
- `docs/00-开发前冻结事项.md`
- `docs/00-CODEX使用说明.md`
- `docs/architecture/`
- `docs/business/`
- `docs/ui/`
- `sql/schema_mysql8.sql`
- `sql/seed_data.sql`
- `openapi/openapi.yaml`
- `tasks/开发任务总清单.md`
- `tasks/TASK-001` 至 `TASK-022`
- `tests/验收测试用例.md`

## 输出物

输出一份：

```text
《工程包预审问题清单》
```

必须按“严重 / 中等 / 一般”分类，并给出是否可以进入 `TASK-001` 的明确判断。

## 完成标准

- 已输出预审问题清单；
- 已说明是否建议进入 `TASK-001`；
- 未修改任何文件；
- 未生成任何代码。
