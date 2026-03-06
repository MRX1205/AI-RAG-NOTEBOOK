<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()

const theme = {
  token: {
    colorPrimary: '#4A90D9',
    colorTextBase: '#2C3E50',
    borderRadius: 8,
    colorBgContainer: '#ffffff',
    colorBorder: '#D6EAF8'
  }
}
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="theme">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
</style>
