# ai-operations 登录与会话接口

## 通用约定

- 以下均为完整网关路径，通过运营网关访问，不再拼接服务前缀。
- JSON 请求体使用 `Content-Type: application/json`。
- 登录、发送短信验证码免认证；其余接口使用 `Authorization: Bearer <sessionToken>`，`Bearer` 大小写固定。
- 不需要租户参数或额外身份请求头。

### 统一响应包装

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | string | 成功为字符串 `"00"`，失败为字符串 `"01"`，不是 HTTP 状态码 |
| message | string | 成功提示或失败原因 |
| data | object / array | 业务数据，类型见各接口；无业务数据或失败时为 null |
| errorCode | string | 业务错误码，成功时省略 |
| errors | array | 错误详情，成功时省略；失败且无详情时为 [] |
| errors.errorCode | string | 每项错误详情的错误码 |
| errors.message | string | 每项错误详情的提示 |

各接口出参表仅列 `data` 的业务字段，不重复包装字段。

### 常见失败处理

- **400**：请求参数或业务校验失败，提示 `message`，必要时展示 `errors`。
- **401**：未登录或会话失效，清除本地令牌并重新登录。
- **500**：服务异常，提示稍后重试。

## 1. 运营用户登录

**POST /ai-operations/api/access/login** · 免认证

说明：支持账号密码或短信验证码登录，返回会话令牌、会话 ID 和用户 ID；同设备登录替换旧会话。

### 入参

JSON 请求体必需。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| loginType | string | 是 | ACCOUNT_PASSWORD（账号密码）或 MOBILE_SMS（短信）；大小写兼容，不接受前后空白 |
| account | string | 条件必填 | 账号密码登录必填，不能为纯空白 |
| password | string | 条件必填 | 账号密码登录必填，不能为纯空白 |
| mobile | string | 条件必填 | 短信登录必填，不能为纯空白；登录不校验手机号正则 |
| smsCode | string | 条件必填 | 短信登录必填，不能为纯空白；无长度或数字正则，按原字符串匹配 |
| deviceId | string | 是 | 16～128 位 ASCII 字母、数字、下划线或连字符，正则 `^[A-Za-z0-9_-]{16,128}$` |

非当前登录方式的条件字段可省略或为 null；字段值不自动去除前后空白。

### 出参

`data` 为 object。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| sessionToken | string | 会话令牌，用于后续 Bearer 认证 |
| sessionId | string | 登录会话 ID |
| userId | string | 运营用户 ID |

## 2. 查询本人登录会话

**GET /ai-operations/api/access/sessions** · 需 Bearer 认证

说明：查询本人有效登录会话，按登录时间倒序；current 标识当前会话，不返回会话令牌。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| — | — | — | 无 |

### 出参

`data` 为 array，可为空数组；以下为每项 object 的全部字段。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| sessionId | string | 会话 ID，可为 null |
| deviceId | string | 登录设备标识，可为 null |
| loginIp | string | 登录来源 IP，可为 null |
| userAgent | string | 登录客户端 User-Agent，可为 null |
| loginAt | string | 本地日期时间，可含小数秒，不带时区，不是 UTC；可为 null |
| current | boolean | 是否为当前会话，不为 null |

## 3. 退出当前登录会话

**POST /ai-operations/api/access/logout** · 需 Bearer 认证

说明：退出当前会话并使其令牌失效，后续使用该令牌访问受保护接口返回 401。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| — | — | — | 无 |

### 出参

`data=null`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| — | — | 无业务字段 |

## 4. 下线本人指定会话

**POST /ai-operations/api/access/sessions/logout** · 需 Bearer 认证

说明：下线本人指定会话，可包括当前会话；会话不存在、不可读或不属于本人时仍返回成功。

### 入参

JSON 请求体必需。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| sessionId | string | 是 | 要下线的本人会话 ID，不能为纯空白 |

### 出参

`data=null`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| — | — | 无业务字段 |

## 5. 退出本人全部登录会话

**POST /ai-operations/api/access/logout-all** · 需 Bearer 认证

说明：退出本人全部会话，包括当前会话，不影响其他用户；旧令牌访问受保护接口返回 401。

### 入参

无需请求体，无查询参数。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| — | — | — | 无 |

### 出参

`data=null`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| — | — | 无业务字段 |

## 6. 发送登录短信验证码

**POST /ai-operations/api/access/sms-code/send** · 免认证

说明：发送登录短信验证码，有效期 5 分钟，同一手机号发送间隔 60 秒，不返回验证码。

### 入参

JSON 请求体必需。

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| mobile | string | 是 | 不能为纯空白，须匹配 `^1[3-9]\d{9}$`；此处有手机号格式校验 |

### 出参

`data=null`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| — | — | 无业务字段 |

联调注意：dev/test 仅模拟短信发送，stag/prod 暂不可用，接口均不回传验证码。

