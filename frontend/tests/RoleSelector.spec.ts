/**
 * Story 1.2 — RoleSelector component tests (AC2).
 * Test Design: docs/qa/assessments/1.2-test-design-20260423.md
 */

import { describe, expect, test } from 'vitest';
import { mount } from '@vue/test-utils';
import RoleSelector from '@/components/auth/RoleSelector.vue';
import { ROLE_OPTIONS, type UserRole } from '@/types/auth';

function mountSelector(modelValue: UserRole | null = null) {
  return mount(RoleSelector, {
    props: { modelValue },
    global: {
      // el-card renders as <div> in tests; stub to a simple container with
      // click-through so we can query by [data-role].
      stubs: {
        'el-card': {
          template: `<div class="el-card" :class="$attrs.class" :data-role="$attrs['data-role']" :style="$attrs.style" @click="$emit('click', $event)"><slot /></div>`,
          inheritAttrs: false,
        },
      },
    },
  });
}

describe('AC2: RoleSelector', () => {
  test('1.2-UNIT-022: renders 6 role cards with label + description', () => {
    const wrapper = mountSelector();
    const cards = wrapper.findAll('.role-card');
    expect(cards).toHaveLength(6);

    ROLE_OPTIONS.forEach((option) => {
      const card = wrapper.find(`[data-role="${option.value}"]`);
      expect(card.exists()).toBe(true);
      expect(card.text()).toContain(option.label);
      expect(card.text()).toContain(option.description);
    });
  });

  test('1.2-UNIT-023: clicking a card emits update:modelValue with that role', async () => {
    const wrapper = mountSelector();
    await wrapper.find('[data-role="MEMBER_RURAL"]').trigger('click');

    expect(wrapper.emitted('update:modelValue')).toBeDefined();
    expect(wrapper.emitted('update:modelValue')![0]).toEqual(['MEMBER_RURAL']);
  });

  test("1.2-UNIT-024: modelValue='RESEARCHER' highlights the matching card", () => {
    const wrapper = mountSelector('RESEARCHER');
    const selected = wrapper.find('[data-role="RESEARCHER"]');
    expect(selected.classes()).toContain('selected');

    const other = wrapper.find('[data-role="MEMBER_URBAN"]');
    expect(other.classes()).not.toContain('selected');
  });

  test('1.2-UNIT-025: single-select semantics — second click replaces first selection', async () => {
    const wrapper = mountSelector();
    await wrapper.find('[data-role="MEMBER_URBAN"]').trigger('click');
    await wrapper.find('[data-role="RESEARCHER"]').trigger('click');

    const events = wrapper.emitted('update:modelValue')!;
    expect(events).toHaveLength(2);
    const last = events[events.length - 1][0];
    expect(last).toBe('RESEARCHER');
    expect(Array.isArray(last)).toBe(false);
  });

  test('1.2-UNIT-026: each card touch target ≥ 44×44 px (mobile baseline)', () => {
    const wrapper = mountSelector();
    const cards = wrapper.findAll('.role-card');
    expect(cards).toHaveLength(6);
    cards.forEach((card) => {
      const style = (card.element as HTMLElement).style;
      expect(style.minWidth).toBe('44px');
      expect(style.minHeight).toBe('44px');
    });
  });
});
