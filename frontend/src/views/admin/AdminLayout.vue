<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Setting, ChatLineSquare, Document, DataAnalysis, ArrowLeft, Bell } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

function handleMenuSelect(index: string) {
  if (index === '/') {
    router.push('/')
  } else {
    router.push(index)
  }
}
</script>

<template>
  <div style="height: 100vh; display: flex; flex-direction: column;">
    <!-- Header -->
    <header style="height: 56px; background: #fff; border-bottom: 1px solid #e5e5e5; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); z-index: 10; flex-shrink: 0;">
      <div style="display: flex; align-items: center; gap: 12px;">
        <div style="width: 32px; height: 32px; background: #1283e9; border-radius: 8px; display: flex; align-items: center; justify-content: center;">
          <span style="color: white; font-size: 18px;">📖</span>
        </div>
        <span style="font-size: 16px; font-weight: 600; color: #1a1a1a;">校共体AI助手</span>
        <el-tag type="warning" size="small">运营后台</el-tag>
      </div>
      <div style="display: flex; align-items: center; gap: 16px;">
        <el-badge :value="3" :max="99">
          <el-button :icon="Bell" circle size="small" />
        </el-badge>
        <el-dropdown>
          <div style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
            <el-avatar :size="32" style="background: #1283e9;">管</el-avatar>
            <span style="font-size: 14px; color: #333;">管理员</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/')">返回前台</el-dropdown-item>
              <el-dropdown-item divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div style="flex: 1; display: flex; overflow: hidden;">
      <!-- Sidebar -->
      <aside style="width: 220px; background: #f7f8fa; border-right: 1px solid #e5e5e5; flex-shrink: 0; display: flex; flex-direction: column;">
        <el-menu
          :default-active="activeMenu"
          @select="handleMenuSelect"
          style="border-right: none; background: transparent; flex: 1;"
        >
          <el-menu-item index="/admin/knowledge">
            <el-icon><Document /></el-icon>
            <span>知识库管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/feedback">
            <el-icon><ChatLineSquare /></el-icon>
            <span>反馈管理</span>
          </el-menu-item>
          <el-menu-item index="" disabled>
            <el-icon><DataAnalysis /></el-icon>
            <span>对话日志</span>
            <el-tag size="small" type="info" style="margin-left: 8px;">开发中</el-tag>
          </el-menu-item>
          <el-menu-item index="" disabled>
            <el-icon><Setting /></el-icon>
            <span>内容配置</span>
            <el-tag size="small" type="info" style="margin-left: 8px;">开发中</el-tag>
          </el-menu-item>
        </el-menu>
        <div style="padding: 16px; border-top: 1px solid #e5e5e5;">
          <el-button text type="primary" @click="router.push('/')" style="width: 100%;">
            <el-icon><ArrowLeft /></el-icon>
            返回前台
          </el-button>
        </div>
      </aside>

      <!-- Main content -->
      <main style="flex: 1; overflow-y: auto; background: #f0f2f5; padding: 20px;">
        <router-view />
      </main>
    </div>
  </div>
</template>
