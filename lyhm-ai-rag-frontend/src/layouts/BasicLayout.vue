<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏（仅在非 hideLayout 页面显示） -->
    <GlobalHeader v-if="!hideLayout" />
    <!-- 主要内容区域 -->
    <a-layout-content :class="hideLayout ? 'full-content' : 'main-content'">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息（仅在非 hideLayout 页面显示） -->
    <GlobalFooter v-if="!hideLayout" />
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()
const hideLayout = computed(() => route.meta?.hideLayout === true)
</script>

<style scoped>
.basic-layout {
  background: none;
}

.main-content {
  padding: 16px;
  background: transparent;
  margin: 0;
  flex: 1;
}

.full-content {
  padding: 0;
  background: none;
  margin: 0;
  flex: 1;
}
</style>
