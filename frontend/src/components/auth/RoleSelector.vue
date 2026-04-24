<script setup lang="ts">
import { ROLE_OPTIONS, type UserRole } from '@/types/auth';

defineProps<{ modelValue: UserRole | null }>();
const emit = defineEmits<{
  (event: 'update:modelValue', value: UserRole): void;
}>();

function pick(role: UserRole) {
  emit('update:modelValue', role);
}
</script>

<template>
  <div class="role-selector" role="radiogroup" aria-label="选择身份">
    <el-card
      v-for="option in ROLE_OPTIONS"
      :key="option.value"
      class="role-card"
      :class="{ selected: modelValue === option.value }"
      :data-role="option.value"
      shadow="hover"
      role="radio"
      :aria-checked="modelValue === option.value"
      :style="{ minWidth: '44px', minHeight: '44px' }"
      @click="pick(option.value)"
    >
      <div class="role-card__label">{{ option.label }}</div>
      <div class="role-card__desc">{{ option.description }}</div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.role-selector {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md, 16px);

  @media (min-width: 768px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.role-card {
  cursor: pointer;
  border: 2px solid transparent;
  border-radius: var(--radius-md, 8px);
  transition: border-color 120ms ease-in-out;

  &.selected {
    border-color: var(--color-brand, #1a73e8);
    background: rgba(26, 115, 232, 0.06);
  }

  &__label {
    font-size: var(--font-size-lg, 16px);
    font-weight: 600;
    color: var(--color-text-primary, #1f2937);
    margin-bottom: var(--spacing-xs, 4px);
  }

  &__desc {
    font-size: var(--font-size-sm, 13px);
    color: var(--color-text-secondary, #4b5563);
  }
}
</style>
