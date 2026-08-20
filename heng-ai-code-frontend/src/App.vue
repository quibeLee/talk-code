<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'

import { check } from '@/api/healthController.ts'

check().then((res) => {
  console.log(res)
})
</script>

<template>
  <BasicLayout />
</template>

<style></style>
