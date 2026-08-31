<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/logo.png" alt="Logo" />
            <h1 class="site-title">Talk Code AI</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：全局模式 + 用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <a-tooltip placement="bottom">
            <template #title>
              工作流模式：由多节点智能体工作流生成（图片收集 → 提示词增强 → 代码生成 → 质检构建），
              应用一旦使用工作流生成将被锁定为工作流模式
            </template>
            <div class="global-mode">
              <span class="global-mode-label">全局模式</span>
              <a-segmented
                v-model:value="globalMode"
                :options="chatGenModeOptions"
                @change="handleChatGenModeChange"
              />
            </div>
          </a-tooltip>
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { HomeOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { type ChatGenMode, getGlobalChatGenMode, setGlobalChatGenMode } from '@/utils/chatGenMode'

// 全局生成模式：标准（classic）/ 工作流（workflow），持久化到 localStorage，
// 对话页发起生成时读取并作为 mode 参数传给 /app/chat/gen/code
const globalMode = ref<ChatGenMode>(getGlobalChatGenMode())
const chatGenModeOptions = [
  { label: '标准模式', value: 'classic' },
  { label: '工作流模式', value: 'workflow' },
]
const handleChatGenModeChange = (value: unknown) => {
  const mode = value as ChatGenMode
  setGlobalChatGenMode(mode)
  message.success(mode === 'workflow' ? '已切换为工作流模式' : '已切换为标准模式')
}

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 48px;
  width: 48px;
}

.site-title {
  margin: 0;
  font-size: 18px;
  color: #1890ff;
}

.ant-menu-horizontal {
  border-bottom: none !important;
}

.user-login-status {
  display: flex;
  align-items: center;
  gap: 16px;
}

.global-mode {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: default;
}

.global-mode-label {
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
}
</style>
