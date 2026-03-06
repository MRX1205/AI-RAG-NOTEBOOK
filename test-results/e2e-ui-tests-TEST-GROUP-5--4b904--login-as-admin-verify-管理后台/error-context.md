# Page snapshot

```yaml
- generic [ref=e1]:
  - main [ref=e4]:
    - generic [ref=e5]:
      - heading "用户登录" [level=2] [ref=e6]
      - generic [ref=e7]:
        - textbox "请输入账号" [ref=e13]: testuser2026
        - generic [ref=e19]:
          - textbox "请输入密码" [active] [ref=e20]: newpass456
          - img "eye-invisible" [ref=e22] [cursor=pointer]:
            - img [ref=e23]
        - generic [ref=e26]:
          - text: 没有账号
          - link "去注册" [ref=e27] [cursor=pointer]:
            - /url: /user/register
        - button "登 录" [ref=e33] [cursor=pointer]:
          - generic [ref=e34]: 登 录
  - generic [ref=e35]:
    - generic "Toggle devtools panel" [ref=e36] [cursor=pointer]:
      - img [ref=e37]
    - generic "Toggle Component Inspector" [ref=e42] [cursor=pointer]:
      - img [ref=e43]
```